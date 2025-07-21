const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

(async () => {
  try {
    console.log('Launching browser...');
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    
    const htmlPath = path.join(__dirname, '..', 'test_pdf.html');
    const pdfPath = path.join(__dirname, '..', 'test_invoice.pdf');
    
    console.log(`Loading HTML from: ${htmlPath}`);
    await page.goto('file://' + htmlPath, { waitUntil: 'networkidle0' });
    
    console.log(`Generating PDF to: ${pdfPath}`);
    await page.pdf({
      path: pdfPath,
      format: 'A4',
      printBackground: true,
      margin: {
        top: '20px',
        right: '20px',
        bottom: '20px',
        left: '20px'
      }
    });
    
    console.log('PDF created successfully!');
    await browser.close();
  } catch (error) {
    console.error('Error generating PDF:', error);
  }
})();
