Nhiệm vụ của các functions trong này là lắng nghe sự thay đổi trên Contract: BHero & BHouse của các mạng: BSC, Polygon.  
Support mainnet hay testnet là tuỳ vào config.

## Cách hoạt động
Sẽ có 1 database lưu trữ:
- Map ((token_id, network) => details)
- Map ((wallet_addr, network) => (token_id, network))

token_id là hero_id hoặc house_id

Sẽ có 1 Redis lưu trữ y như vậy nhưng lifetime chỉ có 5 phút, nhớ set expire cho Redis để tự delete key

### Flow khi có request từ API:
#### B1: Check Redis: Lấy data trong Redis
- Nếu có + chưa expired thì trả về -> Done
- Nếu ko có hoặc expired thì sang B2  
#### B2: Check Database: Lấy data trong Database 
- Nếu có thì trả về Redis -> Done
- Nếu ko có cũng trả về Redis -> Done   
#### B3: Return kết quả về API

### Subscribers:
- Request lên blockchain để lấy owner_addr của token_id (nếu balance có thay đổi)
- Query logs event Transfer(address,address,uint256) để xác minh lại owner_addr <-> token_id

Điều kiện để kích hoạt Subscriber là khi thoả mãn 1 trong các điều kiện sau:
- Mỗi 5 phút
- Có emit event Transfer(address,address,uint256)
- Có emit event TokenCreated(address,uint256,uint256)

Đối với trường hợp token bị burn thì có thể sẽ có event Transfer từ addr -> 0x00 (chưa kiểm chứng)