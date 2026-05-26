# Checklist

- [x] 第1层模糊层浅色 background = Color.White.copy(alpha = 0.5f)
- [x] 第1层模糊层深色 background = Color(0xFF1C1C1E).copy(alpha = 0.5f)
- [x] 模糊层仍然有 blur(30.dp) — 第112行未修改
- [x] 其余 3 层（磨砂渐变、高光描边、内容 Row）代码不变
- [x] MainActivity overlay 结构不变
- [x] 代码静态验证通过 — 仅 1 行修改