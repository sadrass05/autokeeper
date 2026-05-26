# Checklist

- [x] MainActivity.onCreate() 调用 enableEdgeToEdge()
- [x] HomeScreen topBar Text 有 statusBarsPadding()
- [x] RecordsScreen topBar Text 有 statusBarsPadding()
- [x] SettingsScreen topBar Text 有 statusBarsPadding()
- [x] FinanceScreen 顶层 LazyColumn 有 statusBarsPadding()
- [x] themes.xml 含 windowTranslucentStatus=false
- [x] themes.xml 含 windowTranslucentNavigation=false
- [x] 代码静态验证通过（构建环境 Kotlin daemon 沙箱限制，无法执行 Gradle）