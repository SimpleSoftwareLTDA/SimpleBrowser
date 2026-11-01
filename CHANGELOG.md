# Changelog

## 1.1.0 (2026-11-01)

- **Foco em WebView.** O projeto passa a distribuir apenas a variante WebView
  (`geckoExcluded`), usando a WebView do sistema. Motivação: maior penetração e
  distribuição no mercado — a WebView é padrão em todos os Android TV/Google TV,
  dispensa download de engine no APK e mantém o app leve.
- Removida a variante GeckoView (`geckoIncluded`) e o módulo `app/gecko`.
- Simplificado o fluxo de release e o `latest_version.json` (1 APK universal por release).
