#LoRa GPS tracker
* kick-off : 2016-03-16

## Server
* ThingPlug
* FireBase : planed 

## ToDo


### Map Activity

- Delete max distance → Delete all related to the Max something.
- allocate range circle to loraNow device → 우선은 위치 변경 시 마다 업데이트(현재)하는 것으로 할 것. 
 - 카메라 줌을 위치가 바뀔 때 마다 하지 말고 circle 만 update.
 - 현재 위치 아이콘을 누를때에만 camera zoom   

- Mark icon  
**현재 아이콘은 너무 딱딱한 느낌** 
→ 애니메이션 적용된 아이콘 적용에 대해서 알아보고 적용 여부 결정 
→ 3월에는 필요한 기능을 우선 적용하고 나중에 고려할 것.  
 
### Service
- Map activity 관련된 내용외에는 모두 삭제할 것.
#### Notification
**우선은 시간으로만 비교하자**

- Notification 조건을 data의 날짜로 비교하지 말고 이전 데이터와의 거리로 적용할 것,
 - 거리비교해서 이전값과의 거리가 일정 거리 이상일때만 노티하고 이때에만 이전거리에 현재거리를 적용
- Service 시작 시에 현재 시간을 저장하고 최초데이터가 현재시간보다 이후에 생성되었으면 Notification
 
 
 
 
  


