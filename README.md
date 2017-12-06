## Mr Filter 2

Mr Filter allows 
- OfferCreate and OfferCancel transaction
- Non-transaction requests (account_xxx, ledger_xxx, bookoffer subscription, etc)

Mr Filter blocks
- Non-offer transactions (payment, trust set, etc)
- tx_blob

### For User:

To submit an order, user will use the unsafe method [Sign and Submit](https://ripple.com/build/rippled-apis/#sign-and-submit-mode) with `secret`  replaced with Mr Exchange wallet Api key. 

If the Api key matches the secret key in database, the request will be passed on to Rippled with `secret` replaces with the real secret key. If not `secret` will be filled with random string. 

Example

```javascript
{
  "id": 2,
  "command": "submit",
  "tx_json" : {
      "TransactionType" : "OfferCreate",
      "Account" : "rf1BiGeXwwQoi8Z2ueFYTEXSwuJYfV2Jpn"
   },
   "secret" : "aPIKEYxxx123",
   "offline": false
}

```
 
### For Admin:

Create application.conf. Important settings : 
- akka.http.server.idle-timeout = infinite to prevent disconnect. Put the file in `src/main/resources`. 
- redis configuration

User wallet should be stored with format 
account -> ("apiKey" -> apikey, "secretKey" -> secretKey)

where account is hash and apiKey and secretKey are fields. Use HSET. 





This project also contains MrFilter2 in vertx.

v.202 Matched against correct structure

v.201 Working version