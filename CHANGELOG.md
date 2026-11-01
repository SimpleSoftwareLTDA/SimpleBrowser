# Changelog

## 1.1.1 (2026-11-01)

- Alinhado o domínio e o esquema de pareamento entre TV e celular (`https://sb.robsoncassiano.software/sync` e `simplebrowser://sync`).
- Correções de segurança (assinatura do token interno de troca de processo, guarda de origem e limite no bridge de download de blobs, sanitização de nomes de arquivos e neutralização de componentes em intents externos).

## 1.1.0 (2026-11-01)

- **Foco em WebView.** O projeto passa a distribuir apenas a variante WebView
  (`geckoExcluded`), usando a WebView do sistema. Motivação: maior penetração e
  distribuição no mercado, a WebView é padrão em todos os Android TV/Google TV,
  dispensa download de engine no APK e mantém o app leve.
- Removida a variante GeckoView (`geckoIncluded`) e o módulo `app/gecko`.
- Simplificado o fluxo de release e o `latest_version.json` (1 APK universal por release).
