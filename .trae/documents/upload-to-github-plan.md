# AutoKeeper GitHub 上传设计

**日期**: 2026-05-26  
**状态**: 已批准

## 目标

将本地 Android 记账应用项目上传至 GitHub，创建全新公开仓库。

## 配置

| 配置项 | 值 |
|--------|-----|
| 仓库名称 | `autokeeper` |
| 认证方式 | SSH |
| 虚拟环境 | 排除 `.venv` 目录 |
| README | 自动生成 |

## 技术方案

### 方法: GitHub API 创建仓库

使用 GitHub REST API 创建远程仓库，无需安装额外工具（gh CLI）。

**API Endpoint**: `POST /user/repos`

### 流程步骤

1. **初始化本地 Git**
   - 运行 `git init`
   - 创建 `.gitignore`（排除 `.venv`、`.gradle`、`build`、`*.apk` 等）

2. **配置 Git 用户信息**
   - 从本地 Git 配置读取用户名和邮箱

3. **创建 GitHub 仓库**
   - 通过 GitHub API (`https://api.github.com/user/repos`)
   - 使用 SSH URL (`git@github.com:username/autokeeper.git`)
   - 添加远程仓库

4. **提交代码**
   - `git add .`
   - `git commit -m "Initial commit"`

5. **推送到 GitHub**
   - `git push -u origin master` (或 `main`)

6. **创建 README.md**
   - 内容包含：项目名称、简介、技术栈、功能特性
   - 提交并推送

## 忽略文件列表

```
.venv/
*.pyc
__pycache__/
*.apk
*.aab
build/
.gradle/
.idea/
*.iml
local.properties
.DS_Store
*.log
```

## 预计结果

- GitHub 仓库: `https://github.com/<username>/autokeeper`
- 包含完整 Android 项目代码
- 包含 Python Flask 后端代码
- 包含自动生成的 README.md
