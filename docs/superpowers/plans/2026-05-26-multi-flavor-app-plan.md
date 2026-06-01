# 多版本应用实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有应用重构为两个独立版本（标准版和 Pro 版），使用 Android Product Flavors 功能。

**Architecture:** 基于现有的单一代码库，使用 Gradle productFlavors 创建两个构建变种。核心代码共享在 `main/` 源集，特有代码放在 `standard/` 和 `pro/` 源集中。

**Tech Stack:** Android, Kotlin, Jetpack Compose, Gradle, Room, Hilt

---

## 📁 文件变更概览

### 新建文件
- `app/src/standard/java/com/example/autobookkeeper/FlavorConfig.kt`
- `app/src/pro/java/com/example/autobookkeeper/FlavorConfig.kt`
- `app/src/standard/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt`
- `app/src/pro/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt`
- `app/src/standard/res/mipmap-*/ic_launcher.png`（多个分辨率）
- `app/src/standard/res/mipmap-*/ic_launcher_round.png`（多个分辨率）
- `app/src/pro/res/mipmap-*/ic_launcher.png`（多个分辨率）
- `app/src/pro/res/mipmap-*/ic_launcher_round.png`（多个分辨率）

### 移动文件
- `app/src/main/java/com/example/autobookkeeper/data/entity/FinancePosition.kt` → `app/src/pro/java/com/example/autobookkeeper/data/entity/FinancePosition.kt`
- `app/src/main/java/com/example/autobookkeeper/data/entity/FinanceExpense.kt` → `app/src/pro/java/com/example/autobookkeeper/data/entity/FinanceExpense.kt`
- `app/src/main/java/com/example/autobookkeeper/data/dao/FinancePositionDao.kt` → `app/src/pro/java/com/example/autobookkeeper/data/dao/FinancePositionDao.kt`
- `app/src/main/java/com/example/autobookkeeper/data/dao/FinanceExpenseDao.kt` → `app/src/pro/java/com/example/autobookkeeper/data/dao/FinanceExpenseDao.kt`
- `app/src/main/java/com/example/autobookkeeper/data/repository/FinanceRepository.kt` → `app/src/pro/java/com/example/autobookkeeper/data/repository/FinanceRepository.kt`
- `app/src/main/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt` → `app/src/pro/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt`

### 修改文件
- `app/build.gradle`
- `app/src/main/java/com/example/autobookkeeper/data/AppDatabase.kt`（拆分为两个版本）
- `app/src/main/java/com/example/autobookkeeper/ui/MainActivity.kt`
- `app/src/main/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt`（移除，拆分为两个版本）
- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`

### 删除文件
- `app/src/main/res/mipmap-*/ic_launcher.xml`（之前已删除）
- `app/src/main/res/mipmap-*/ic_launcher_round.xml`（之前已删除）

---

## 📝 具体任务

---

### Task 1: 配置 Product Flavors

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: 备份现有 build.gradle**

```bash
cd "a:\study\Trae CN\work\money"
copy app\build.gradle app\build.gradle.backup
```

- [ ] **Step 2: 修改 build.gradle 添加 productFlavors**

编辑 `app/build.gradle`，在 `android` 块中添加以下内容：

```gradle
flavorDimensions "version"
productFlavors {
    standard {
        dimension "version"
        applicationId "com.example.autobookkeeper"
        versionName "1.0"
        resValue "string", "app_name", "自动记账助手"
    }
    pro {
        dimension "version"
        applicationId "com.example.autobookkeeper.pro"
        versionName "1.0-pro"
        resValue "string", "app_name", "自动记账助手 Pro"
    }
}
```

确保这部分添加在 `defaultConfig` 之后、`buildTypes` 之前。

- [ ] **Step 3: 验证配置可以同步**

```bash
cd "a:\study\Trae CN\work\money"
.\gradlew tasks --group=Build
```

Expected: 能看到 standard 和 pro 相关的任务出现。

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle
git commit -m "feat: add product flavors for standard and pro versions"
```

---

### Task 2: 创建 flavor 源集目录结构

**Files:**
- Create: `app/src/standard/` 目录
- Create: `app/src/pro/` 目录

- [ ] **Step 1: 创建目录结构**

```bash
cd "a:\study\Trae CN\work\money\app\src"
mkdir standard
mkdir standard\java
mkdir standard\java\com
mkdir standard\java\com\example
mkdir standard\java\com\example\autobookkeeper
mkdir standard\res

mkdir pro
mkdir pro\java
mkdir pro\java\com
mkdir pro\java\com\example
mkdir pro\java\com\example\autobookkeeper
mkdir pro\res
```

- [ ] **Step 2: 创建资源子目录**

```bash
cd "a:\study\Trae CN\work\money\app\src"
mkdir standard\res\mipmap-mdpi
mkdir standard\res\mipmap-hdpi
mkdir standard\res\mipmap-xhdpi
mkdir standard\res\mipmap-xxhdpi
mkdir standard\res\mipmap-xxxhdpi

mkdir pro\res\mipmap-mdpi
mkdir pro\res\mipmap-hdpi
mkdir pro\res\mipmap-xhdpi
mkdir pro\res\mipmap-xxhdpi
mkdir pro\res\mipmap-xxxhdpi
```

- [ ] **Step 3: 验证目录结构**

```bash
cd "a:\study\Trae CN\work\money\app\src"
dir /s /b
```

Expected: 看到完整的 standard 和 pro 目录结构。

- [ ] **Step 4: Commit**

```bash
git add app/src/standard
git add app/src/pro
git commit -m "feat: create standard and pro source set directories"
```

---

### Task 3: 创建 FlavorConfig 配置接口

**Files:**
- Create: `app/src/main/java/com/example/autobookkeeper/FlavorConfig.kt`
- Create: `app/src/standard/java/com/example/autobookkeeper/FlavorConfigImpl.kt`
- Create: `app/src/pro/java/com/example/autobookkeeper/FlavorConfigImpl.kt`

- [ ] **Step 1: 创建共享接口 FlavorConfig.kt**

```kotlin
package com.example.autobookkeeper

interface FlavorConfig {
    val hasFinanceFeature: Boolean
    val navItemCount: Int
    val appName: String
}
```

保存到 `app/src/main/java/com/example/autobookkeeper/FlavorConfig.kt`

- [ ] **Step 2: 创建标准版实现 FlavorConfigImpl.kt**

```kotlin
package com.example.autobookkeeper

object FlavorConfigImpl : FlavorConfig {
    override val hasFinanceFeature: Boolean = false
    override val navItemCount: Int = 3
    override val appName: String = "自动记账助手"
}
```

保存到 `app/src/standard/java/com/example/autobookkeeper/FlavorConfigImpl.kt`

- [ ] **Step 3: 创建 Pro 版实现 FlavorConfigImpl.kt**

```kotlin
package com.example.autobookkeeper

object FlavorConfigImpl : FlavorConfig {
    override val hasFinanceFeature: Boolean = true
    override val navItemCount: Int = 4
    override val appName: String = "自动记账助手 Pro"
}
```

保存到 `app/src/pro/java/com/example/autobookkeeper/FlavorConfigImpl.kt`

- [ ] **Step 4: 验证文件结构**

检查三个文件是否在正确的位置。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/autobookkeeper/FlavorConfig.kt
git add app/src/standard/java/com/example/autobookkeeper/FlavorConfigImpl.kt
git add app/src/pro/java/com/example/autobookkeeper/FlavorConfigImpl.kt
git commit -m "feat: add FlavorConfig interface and implementations"
```

---

### Task 4: 移动理财相关代码到 pro 源集

**Files:**
- Move: `app/src/main/java/com/example/autobookkeeper/data/entity/FinancePosition.kt` → `app/src/pro/java/com/example/autobookkeeper/data/entity/FinancePosition.kt`
- Move: `app/src/main/java/com/example/autobookkeeper/data/entity/FinanceExpense.kt` → `app/src/pro/java/com/example/autobookkeeper/data/entity/FinanceExpense.kt`
- Move: `app/src/main/java/com/example/autobookkeeper/data/dao/FinancePositionDao.kt` → `app/src/pro/java/com/example/autobookkeeper/data/dao/FinancePositionDao.kt`
- Move: `app/src/main/java/com/example/autobookkeeper/data/dao/FinanceExpenseDao.kt` → `app/src/pro/java/com/example/autobookkeeper/data/dao/FinanceExpenseDao.kt`
- Move: `app/src/main/java/com/example/autobookkeeper/data/repository/FinanceRepository.kt` → `app/src/pro/java/com/example/autobookkeeper/data/repository/FinanceRepository.kt`
- Move: `app/src/main/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt` → `app/src/pro/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt`

- [ ] **Step 1: 创建目标目录**

```bash
cd "a:\study\Trae CN\work\money\app\src\pro\java\com\example\autobookkeeper"
mkdir data
mkdir data\entity
mkdir data\dao
mkdir data\repository
mkdir ui
mkdir ui\screen
```

- [ ] **Step 2: 移动实体文件**

```bash
cd "a:\study\Trae CN\work\money"
move app\src\main\java\com\example\autobookkeeper\data\entity\FinancePosition.kt app\src\pro\java\com\example\autobookkeeper\data\entity\FinancePosition.kt
move app\src\main\java\com\example\autobookkeeper\data\entity\FinanceExpense.kt app\src\pro\java\com\example\autobookkeeper\data\entity\FinanceExpense.kt
```

- [ ] **Step 3: 移动 DAO 文件**

```bash
move app\src\main\java\com\example\autobookkeeper\data\dao\FinancePositionDao.kt app\src\pro\java\com\example\autobookkeeper\data\dao\FinancePositionDao.kt
move app\src\main\java\com\example\autobookkeeper\data\dao\FinanceExpenseDao.kt app\src\pro\java\com\example\autobookkeeper\data\dao\FinanceExpenseDao.kt
```

- [ ] **Step 4: 移动 Repository 文件**

```bash
move app\src\main\java\com\example\autobookkeeper\data\repository\FinanceRepository.kt app\src\pro\java\com\example\autobookkeeper\data\repository\FinanceRepository.kt
```

- [ ] **Step 5: 移动 FinanceScreen**

```bash
move app\src\main\java\com\example\autobookkeeper\ui\screen\FinanceScreen.kt app\src\pro\java\com\example\autobookkeeper\ui\screen\FinanceScreen.kt
```

- [ ] **Step 6: 验证文件移动**

检查源目录中是否已移除这些文件，目标目录中是否存在。

- [ ] **Step 7: Commit**

```bash
git add app/src/pro/java/com/example/autobookkeeper/data/entity/FinancePosition.kt
git add app/src/pro/java/com/example/autobookkeeper/data/entity/FinanceExpense.kt
git add app/src/pro/java/com/example/autobookkeeper/data/dao/FinancePositionDao.kt
git add app/src/pro/java/com/example/autobookkeeper/data/dao/FinanceExpenseDao.kt
git add app/src/pro/java/com/example/autobookkeeper/data/repository/FinanceRepository.kt
git add app/src/pro/java/com/example/autobookkeeper/ui/screen/FinanceScreen.kt
git add -u
git commit -m "feat: move finance related code to pro source set"
```

---

### Task 5: 拆分 AppDatabase 为两个版本

**Files:**
- Modify: `app/src/main/java/com/example/autobookkeeper/data/AppDatabase.kt` → 拆分
- Create: `app/src/standard/java/com/example/autobookkeeper/data/AppDatabase.kt`
- Create: `app/src/pro/java/com/example/autobookkeeper/data/AppDatabase.kt`

- [ ] **Step 1: 先读取并备份现有 AppDatabase.kt**

先读取现有文件内容备份。

- [ ] **Step 2: 创建标准版 AppDatabase.kt**

```kotlin
package com.example.autobookkeeper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.ExpenseRecord

@Database(
    entities = [
        Category::class,
        ExpenseRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autobookkeeper_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

保存到 `app/src/standard/java/com/example/autobookkeeper/data/AppDatabase.kt`

- [ ] **Step 3: 创建 Pro 版 AppDatabase.kt**

使用原版内容，包含理财表：

```kotlin
package com.example.autobookkeeper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.dao.ExpenseDao
import com.example.autobookkeeper.data.dao.FinancePositionDao
import com.example.autobookkeeper.data.dao.FinanceExpenseDao
import com.example.autobookkeeper.data.entity.Category
import com.example.autobookkeeper.data.entity.ExpenseRecord
import com.example.autobookkeeper.data.entity.FinancePosition
import com.example.autobookkeeper.data.entity.FinanceExpense

@Database(
    entities = [
        Category::class,
        ExpenseRecord::class,
        FinancePosition::class,
        FinanceExpense::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun financePositionDao(): FinancePositionDao
    abstract fun financeExpenseDao(): FinanceExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autobookkeeper_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

保存到 `app/src/pro/java/com/example/autobookkeeper/data/AppDatabase.kt`

- [ ] **Step 4: 删除 main 中的 AppDatabase.kt**

```bash
cd "a:\study\Trae CN\work\money"
del app\src\main\java\com\example\autobookkeeper\data\AppDatabase.kt
```

- [ ] **Step 5: Commit**

```bash
git add app/src/standard/java/com/example/autobookkeeper/data/AppDatabase.kt
git add app/src/pro/java/com/example/autobookkeeper/data/AppDatabase.kt
git add -u
git commit -m "feat: split AppDatabase for standard and pro versions"
```

---

### Task 6: 拆分 MainViewModel

**Files:**
- Modify: `app/src/main/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt` → 删除
- Create: `app/src/standard/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt`
- Create: `app/src/pro/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt`

- [ ] **Step 1: 先读取并备份现有 MainViewModel.kt**

读取现有内容作为 Pro 版基础。

- [ ] **Step 2: 创建目标目录**

```bash
cd "a:\study\Trae CN\work\money\app\src\standard\java\com\example\autobookkeeper"
mkdir ui
mkdir ui\viewmodel

cd "a:\study\Trae CN\work\money\app\src\pro\java\com\example\autobookkeeper"
mkdir ui
mkdir ui\viewmodel
```

- [ ] **Step 3: 创建标准版 MainViewModel.kt**

创建不含理财功能的 ViewModel 版本（移除所有 Finance 相关代码）。

- [ ] **Step 4: 创建 Pro 版 MainViewModel.kt**

使用原版完整代码作为 Pro 版。

- [ ] **Step 5: 删除 main 中的 MainViewModel.kt**

```bash
cd "a:\study\Trae CN\work\money"
del app\src\main\java\com\example\autobookkeeper\ui\viewmodel\MainViewModel.kt
```

- [ ] **Step 6: Commit**

```bash
git add app/src/standard/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt
git add app/src/pro/java/com/example/autobookkeeper/ui/viewmodel/MainViewModel.kt
git add -u
git commit -m "feat: split MainViewModel for standard and pro versions"
```

---

### Task 7: 修改 MainActivity 使用 FlavorConfig

**Files:**
- Modify: `app/src/main/java/com/example/autobookkeeper/MainActivity.kt`

- [ ] **Step 1: 读取现有 MainActivity.kt**

获取当前内容。

- [ ] **Step 2: 修改 MainActivity 使用 FlavorConfig**

更新导航逻辑，根据 `FlavorConfigImpl.hasFinanceFeature` 来决定是否显示理财 tab。

- [ ] **Step 3: 保存修改**

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/autobookkeeper/MainActivity.kt
git commit -m "feat: update MainActivity to use FlavorConfig"
```

---

### Task 8: 修改 HomeScreen 移除理财相关显示

**Files:**
- Modify: `app/src/main/java/com/example/autobookkeeper/ui/screen/HomeScreen.kt`

- [ ] **Step 1: 读取现有 HomeScreen.kt**

- [ ] **Step 2: 使用 FlavorConfig 判断是否显示理财相关内容**

包装理财相关的 UI 显示，使用 `FlavorConfigImpl.hasFinanceFeature` 来判断。

- [ ] **Step 3: 保存修改**

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/autobookkeeper/ui/screen/HomeScreen.kt
git commit -m "feat: conditionally show finance features based on flavor"
```

---

### Task 9: 修改 DatabaseModule 适配两个版本

**Files:**
- Modify: `app/src/main/java/com/example/autobookkeeper/di/DatabaseModule.kt`

- [ ] **Step 1: 读取现有 DatabaseModule.kt**

- [ ] **Step 2: 修改 DatabaseModule**

确保注入代码可以兼容两个版本的 AppDatabase。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/autobookkeeper/di/DatabaseModule.kt
git commit -m "feat: update DatabaseModule for multi-flavor"
```

---

### Task 10: 配置图标资源

**Files:**
- Create: 多个 mipmap 目录下的图标文件

- [ ] **Step 1: 创建占位说明文件**

在 `app/src/standard/res/mipmap-*/` 和 `app/src/pro/res/mipmap-*/` 目录下创建说明文件，告诉用户把 PNG 图标放在这里。

- [ ] **Step 2: 更新 custom_icons 文档**

更新 `custom_icons/README.md`，添加多版本图标的使用说明。

- [ ] **Step 3: Commit**

```bash
git add app/src/standard/res/
git add app/src/pro/res/
git add custom_icons/README.md
git commit -m "feat: set up icon resources for both versions"
```

---

### Task 11: 更新 AndroidManifest.xml

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 更新应用名称引用**

将硬编码的应用名称改为使用 gradle 定义的 `app_name` 字符串资源。

- [ ] **Step 2: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: update AndroidManifest for multi-flavor"
```

---

### Task 12: 测试编译两个版本

**Files:**
- Test: build outputs

- [ ] **Step 1: 清理并编译标准版**

```bash
cd "a:\study\Trae CN\work\money"
.\gradlew clean
.\gradlew assembleStandardDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 清理并编译 Pro 版**

```bash
.\gradlew clean
.\gradlew assembleProDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 验证 APK 文件存在**

检查 `app/build/outputs/apk/` 目录下是否有两个版本的 APK。

- [ ] **Step 4: Commit (if any fixes)**

如果过程中有需要修复的问题，提交修复。

---

### Task 13: 最终验证和文档更新

**Files:**
- Create/Update: `docs/BUILDING.md`

- [ ] **Step 1: 创建构建说明文档**

创建 `docs/BUILDING.md`，说明如何构建两个版本的应用。

- [ ] **Step 2: 更新主 README**

更新 `README.md` 中的说明。

- [ ] **Step 3: Commit**

```bash
git add docs/BUILDING.md
git add README.md
git commit -m "docs: add multi-flavor build instructions"
```

---

## ✅ 验收测试

执行以下手动测试：

- [ ] 标准版 APK 可以安装
- [ ] 标准版没有理财功能（底部只有 3 个 tab）
- [ ] Pro 版 APK 可以安装
- [ ] Pro 版有完整功能（底部有 4 个 tab）
- [ ] 两个版本可以同时安装在同一设备上
- [ ] 两个版本使用不同的应用名称
- [ ] 两个版本使用不同的图标

---

## 📝 注意事项

1. **图标配置**: 用户需要自己准备并放置两个版本的 PNG 图标
2. **数据迁移**: 因为应用 ID 不同，两个版本的数据不共享
3. **Hilt 依赖注入**: 确保 Hilt 模块在两个版本中都能正常工作
4. **持续开发**: 后续开发时要注意代码应该放在哪个源集中
