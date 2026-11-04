/* Gera o PDF do relatório de entrega usando Chromium via Playwright. */
const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..', '..', '..')
const outPdf = path.join(projectRoot, 'docs', 'entrega', 'RELATORIO_ENTREGA.pdf')
const playwright = require(path.join(projectRoot, 'frontend', 'node_modules', '@playwright', 'test'))
const { chromium } = playwright
const htmlPath = process.argv[2]

;(async () => {
  const html = fs.readFileSync(htmlPath, 'utf8')
  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.setContent(html, { waitUntil: 'networkidle' })
  await page.pdf({
    path: outPdf,
    format: 'A4',
    printBackground: true,
    margin: { top: '18mm', bottom: '18mm', left: '16mm', right: '16mm' },
    headerTemplate: '<div style="width:100%;font-size:8px;color:#777;padding:0 12mm;">Rede Krill — Relatório de Entrega</div>',
    footerTemplate: '<div style="width:100%;font-size:8px;color:#777;text-align:center;"><span class="pageNumber"></span>/<span class="totalPages"></span></div>'
  })
  await browser.close()
  console.log('OK', outPdf)
})().catch((e) => { console.error(e); process.exit(1) })
