#!/usr/bin/env bash
# Deploy the static site (landing, placeholders and nginx configs) and,
# optionally, the signed release APK to the web server.
#
# The whole flow is SSH-free: artifacts go to S3 and the instance pulls them
# with its instance profile, then nginx is reloaded through SSM.
#
# Usage:
#   ./tools/deploy-site.sh                  # site only (web/ + ops/nginx/)
#   ./tools/deploy-site.sh --apk            # site + release APK + SHA256SUMS
#   ./tools/deploy-site.sh --apk --duckdns  # + force the DuckDNS IP update
#
# Environment:
#   INSTANCE_ID      SSM instance id   (default i-0bf2980671102b46f)
#   AWS_REGION       region            (default us-east-2)
#   ARTIFACTS_BUCKET artifact bucket   (default mainboard-override-artifacts-689217346963)
#   DUCKDNS_TOKEN    duckdns.org token (required with --duckdns; if unset it
#                    is read from the gitignored .duckdns_token file). The
#                    update always pins the *server's* public IP, never the
#                    caller's, so the records never drift.
#   DUCKDNS_DOMAINS  comma list without suffix
#                    (default mainboard-override,nexxxusapp,nexxus-api)
#   KEEP_TMP=1       keep the staging dir (params.json, tar) for debugging
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INSTANCE_ID="${INSTANCE_ID:-i-0bf2980671102b46f}"
AWS_REGION="${AWS_REGION:-us-east-2}"
BUCKET="${ARTIFACTS_BUCKET:-mainboard-override-artifacts-689217346963}"
DUCKDNS_DOMAINS="${DUCKDNS_DOMAINS:-mainboard-override,nexxxusapp,nexxus-api}"

WITH_APK=0
WITH_DUCKDNS=0
for arg in "$@"; do
    case "$arg" in
        --apk) WITH_APK=1 ;;
        --duckdns) WITH_DUCKDNS=1 ;;
        -h|--help) sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "error: unknown argument: $arg" >&2; exit 2 ;;
    esac
done

command -v aws >/dev/null || { echo "error: aws CLI not found in PATH" >&2; exit 1; }
command -v curl >/dev/null || { echo "error: curl not found in PATH" >&2; exit 1; }

STAGE="$(mktemp -d /tmp/deploy-site.XXXXXX)"
if [ "${KEEP_TMP:-0}" != "1" ]; then
    trap 'rm -rf "$STAGE"' EXIT
fi

VERSION="$(grep -oP 'versionName = "\K[^"]+' "$ROOT/app/build.gradle.kts")"
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
APK_NAME="mainboard-override-$VERSION.apk"

# --- 1) release APK: sync the landing's version, size and checksum --------
if [ "$WITH_APK" = 1 ]; then
    [ -f "$APK" ] || {
        echo "error: $APK not found (run ./gradlew :app:assembleRelease first)" >&2
        exit 1
    }
    SHA="$(sha256sum "$APK" | cut -d' ' -f1)"
    SIZE_MB="$((($(stat -c%s "$APK") + 1048575) / 1048576))"
    echo "apk    : $APK_NAME | $SIZE_MB MB | sha256 $SHA"
    sed -i -E \
        -e "s#mainboard-override-[0-9]+\.[0-9]+\.[0-9]+\.apk#$APK_NAME#g" \
        -e "s#v[0-9]+\.[0-9]+\.[0-9]+#v$VERSION#g" \
        -e "s#Descargar APK · [0-9.]+#Descargar APK · $VERSION#g" \
        -e "s#^[0-9a-f]{64}\b#$SHA#g" \
        -e "s#[0-9]+ MB · paquete#$SIZE_MB MB · paquete#g" \
        "$ROOT/web/index.html"
fi

# --- 2) pack web/ + ops/nginx/ into the server's root layout --------------
mkdir -p "$STAGE/pkg/var/www/mainboard-override/downloads" \
         "$STAGE/pkg/var/www/nexxxusapp" \
         "$STAGE/pkg/var/www/nexxus-api" \
         "$STAGE/pkg/etc/nginx/conf.d"
cp "$ROOT/web/index.html" "$ROOT/web/site.css" \
   "$ROOT/web/logo.png" "$ROOT/web/favicon.png" \
   "$STAGE/pkg/var/www/mainboard-override/"
cp "$ROOT"/web/nexxxusapp/* "$STAGE/pkg/var/www/nexxxusapp/"
cp "$ROOT"/web/nexxus-api/*  "$STAGE/pkg/var/www/nexxus-api/"
cp "$ROOT"/ops/nginx/*.conf  "$STAGE/pkg/etc/nginx/conf.d/"
tar -czf "$STAGE/site.tar.gz" -C "$STAGE/pkg" etc var
TAR_SHA="$(sha256sum "$STAGE/site.tar.gz" | cut -d' ' -f1)"
echo "site   : $(stat -c%s "$STAGE/site.tar.gz") bytes | sha256 $TAR_SHA"

# --- 3) upload the artifacts ---------------------------------------------
aws s3 cp "$STAGE/site.tar.gz" "s3://$BUCKET/artifacts/site.tar.gz" \
    --region "$AWS_REGION" --only-show-errors
if [ "$WITH_APK" = 1 ]; then
    aws s3 cp "$APK" "s3://$BUCKET/artifacts/$APK_NAME" \
        --region "$AWS_REGION" --only-show-errors
fi

# --- 4) build the SSM command (no double quotes inside the lines) ---------
PARAMS="$STAGE/params.json"
cat > "$PARAMS" <<EOF
{
  "commands": [
    "set -e",
    "aws s3 cp s3://$BUCKET/artifacts/site.tar.gz /tmp/site.tar.gz --region $AWS_REGION --only-show-errors",
    "echo '$TAR_SHA  /tmp/site.tar.gz' | sha256sum -c -",
    "tar -xzf /tmp/site.tar.gz -C / && rm -f /tmp/site.tar.gz",
    "chmod 644 /etc/nginx/conf.d/*.conf",
EOF
if [ "$WITH_APK" = 1 ]; then
    cat >> "$PARAMS" <<EOF
    "aws s3 cp s3://$BUCKET/artifacts/$APK_NAME /var/www/mainboard-override/downloads/$APK_NAME --region $AWS_REGION --only-show-errors",
    "cd /var/www/mainboard-override/downloads && echo '$SHA  $APK_NAME' | sha256sum -c -",
    "cd /var/www/mainboard-override/downloads && sha256sum mainboard-override-*.apk > SHA256SUMS",
EOF
fi
cat >> "$PARAMS" <<EOF
    "ln -sfn $APK_NAME /var/www/mainboard-override/downloads/latest.apk",
    "nginx -t",
    "systemctl reload nginx",
    "sleep 1",
    "curl -skI --resolve mainboard-override.duckdns.org:443:127.0.0.1 https://mainboard-override.duckdns.org/ | head -1",
    "curl -skI --resolve nexxxusapp.duckdns.org:443:127.0.0.1 https://nexxxusapp.duckdns.org/ | head -1",
    "curl -skI --resolve nexxus-api.duckdns.org:443:127.0.0.1 https://nexxus-api.duckdns.org/ | head -1",
    "curl -sS -m 5 -o /dev/null -w 'unknown-host=%{http_code}' --resolve intruso.example:443:127.0.0.1 https://intruso.example/ || echo unknown-host=handshake-rejected"
  ]
}
EOF

# --- 5) run it through SSM and wait --------------------------------------
CMD_ID="$(aws ssm send-command --instance-ids "$INSTANCE_ID" \
    --document-name AWS-RunShellScript --parameters "file://$PARAMS" \
    --region "$AWS_REGION" --query 'Command.CommandId' --output text)"
echo "ssm    : $CMD_ID"

STATUS="InProgress"
for _ in $(seq 1 180); do
    STATUS="$(aws ssm get-command-invocation --command-id "$CMD_ID" \
        --instance-id "$INSTANCE_ID" --region "$AWS_REGION" \
        --query Status --output text 2>/dev/null || echo InProgress)"
    case "$STATUS" in Success|Cancelled|TimedOut|Failed) break ;; esac
    sleep 5
done
echo "status : $STATUS"
aws ssm get-command-invocation --command-id "$CMD_ID" \
    --instance-id "$INSTANCE_ID" --region "$AWS_REGION" \
    --query '[StandardOutputContent,StandardErrorContent]' --output text
[ "$STATUS" = "Success" ] || { echo "error: deploy failed ($STATUS)" >&2; exit 1; }

# --- 6) verify over public HTTPS ----------------------------------------
IP="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
    --region "$AWS_REGION" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"
echo "server: http://$IP/ (HTTPS vía DNS público)"
FAILED=0
for d in ${DUCKDNS_DOMAINS//,/ }; do
    CODE="$(curl -s -o /dev/null -w '%{http_code}' -m 15 \
        "https://$d.duckdns.org/")"
    echo "  https://$d.duckdns.org -> $CODE"
    [ "$CODE" = "200" ] || FAILED=1
done

# --- 7) optional DuckDNS IP update (siempre fija la IP del servidor) -----
if [ "$WITH_DUCKDNS" = 1 ]; then
    if [ -z "${DUCKDNS_TOKEN:-}" ] && [ -f "$ROOT/.duckdns_token" ]; then
        DUCKDNS_TOKEN="$(head -n1 "$ROOT/.duckdns_token")"
    fi
    : "${DUCKDNS_TOKEN:?set DUCKDNS_TOKEN (env or .duckdns_token file) or drop --duckdns}"
    for d in ${DUCKDNS_DOMAINS//,/ }; do
        RESP="$(curl -fsS -m 15 \
            "https://www.duckdns.org/update?domains=$d&ip=$IP&token=$DUCKDNS_TOKEN")"
        echo "duckdns: $d -> $IP ($RESP)"
        [ "$RESP" = "OK" ] || { echo "error: duckdns update failed for $d" >&2; FAILED=1; }
    done
fi

[ "$FAILED" = "0" ] || { echo "error: verification failed" >&2; exit 1; }
echo "done   : site deployed and verified"
