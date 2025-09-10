# spring-suggest

## Testing

```bash
curl -G "http://localhost:8083/suggest" \
  --data-urlencode "q=kuber" \
  --data-urlencode "limit=2" \
  -H "X-Session: s1"
```
