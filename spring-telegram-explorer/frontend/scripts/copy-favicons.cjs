const fs = require('fs');
const path = require('path');

const sourceDir = path.join(__dirname, '..', 'favicon');
const targetDir = path.join(__dirname, '..', 'public');

if (!fs.existsSync(sourceDir)) {
  console.error(`Source favicon directory not found: ${sourceDir}`);
  process.exit(1);
}

fs.mkdirSync(targetDir, { recursive: true });

fs.cpSync(sourceDir, targetDir, { recursive: true });

console.log('Favicons copied to public directory');
