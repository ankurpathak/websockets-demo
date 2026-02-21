curl -k -v --alt-svc /tmp/altcache https://demo.local:8443
curl -k -v --http3 https://demo.local:8443
netstat -vanp tcp | grep 8443
 lsof -i tcp:8443