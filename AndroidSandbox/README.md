# 새 화면 추가 방법
```
1. Route.kt에 Route 추가:
   data object NewFeature : Route("new_feature")
   
2. HomeScreen.kt의 menuItems에 추가:
   MenuItem(
      title = "New Feature",
      route = Route.NewFeature
   )
   
3. AppNavHost.kt에 composable 추가:
   composable(Route.NewFeature.route) {
      NewFeatureScreen()
   }
```

# org.webrtc:google-webrtc를 사용하지 않는이유 
기존 라이브러리 :
org.webrtc:google-webrtc:1.0.32006
- Google이 WebRTC를 Maven에 공식 배포하지 않음
- 그래서 Google Maven, Maven Central 어디에도 없음
- 에러 메시지에서 "Could not find" 나온 이유

대체 라이브러리:
io.getstream:stream-webrtc-android:1.1.1
- WebRTC를 미리 빌드해서 Maven Central에 배포한 라이브러리
- GetStream.io에서 관리 (신뢰할 수 있음)
- 별도 저장소 설정 없이 바로 사용 가능

---

