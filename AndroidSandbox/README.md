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