"""Export version-matched Portylus sources, including complete MuPDF sources. Python 3.10+."""
from pathlib import Path
import hashlib, json, tarfile, urllib.request, zipfile

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root/'third_party/mupdf/source-manifest.json').read_text(encoding='utf-8'))
version = dict(line.split('=',1) for line in (root/'version.properties').read_text(encoding='utf-8').splitlines() if '=' in line)['versionName']
up = root/'upstream' if (root/'upstream').is_dir() else root/'artifacts/upstream'
up.mkdir(parents=True, exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for item in manifest['archives']:
    path=up/item['file']
    if not path.exists():
        req=urllib.request.Request(item['url'], headers={'User-Agent':'Portylus-source-export'})
        with urllib.request.urlopen(req, timeout=60) as response, path.open('wb') as out:
            import shutil
            shutil.copyfileobj(response,out)
    if sha(path)!=item['sha256']: raise SystemExit('Source hash mismatch: '+path.name)
with tarfile.open(up/'mupdf-1.28.5-source.tar.gz') as tar:
    if len(tar.getmembers())<8000: raise SystemExit('Incomplete MuPDF source archive')
out=root/f'artifacts/Portylus-v{version}-source.zip'
out.parent.mkdir(parents=True, exist_ok=True)
if out.exists(): raise SystemExit('Source archive already exists. Preserve it or choose a new version before export.')
root_files=['README.md','LICENSE','THIRD_PARTY_NOTICES.md','.gitignore','settings.gradle.kts','build.gradle.kts',
            'gradle.properties','gradlew','gradlew.bat','version.properties','distribution.properties']
files=[root/p for p in root_files]
for folder in ['app/src','gradle','scripts','third_party','Portylus_BrandKit','.github','docs']:
    files.extend(f for f in (root/folder).rglob('*') if f.is_file() and '__pycache__' not in f.parts)
files.append(root/'app/build.gradle.kts')
with zipfile.ZipFile(out,'x',zipfile.ZIP_DEFLATED) as z:
    for f in sorted(set(files)):
        if f.suffix.lower() in {'.jks','.keystore','.p12','.pfx','.clixml'}: raise SystemExit('Unexpected key in source files')
        z.write(f,'Portylus/'+f.relative_to(root).as_posix())
    for item in manifest['archives']:
        z.write(up/item['file'],'Portylus/upstream/'+item['file'],compress_type=zipfile.ZIP_STORED)
with zipfile.ZipFile(out) as z:
    if z.testzip(): raise SystemExit('Invalid output ZIP')
checksum=sha(out)
out.with_suffix('.zip.sha256').write_text(checksum+'  '+out.name+'\n')
print(json.dumps({'file':str(out),'bytes':out.stat().st_size,'sha256':checksum}))
