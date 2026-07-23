import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Remove definitions
    content = re.sub(r'val SurfaceDarkManage = Color\(0xFF141416\)\n', '', content)
    content = re.sub(r'val CardDarkManage = Color\(0xFF1E1E22\)\n', '', content)
    content = re.sub(r'val CardBorderManage = Color\(0xFF2E2E32\)\n', '', content)
    content = re.sub(r'val InputBgManage = Color\(0xFF161618\)\n', '', content)

    # 2. Replace usages
    content = content.replace('SurfaceDarkManage', 'MaterialTheme.colorScheme.background')
    content = content.replace('CardDarkManage', 'MaterialTheme.colorScheme.surface')
    content = content.replace('CardBorderManage', 'MaterialTheme.colorScheme.outlineVariant')
    content = content.replace('InputBgManage', 'MaterialTheme.colorScheme.surfaceVariant')
    
    # 3. Specific components
    # Button texts that should be onPrimary
    # For example: Text("Simpan", color = Color.White
    content = re.sub(r'Text\("Simpan",\s*color\s*=\s*Color\.White', 'Text("Simpan", color = MaterialTheme.colorScheme.onPrimary', content)
    content = re.sub(r'Text\("OK",\s*color\s*=\s*Color\(0xFF3B82F6\)', 'Text("OK", color = MaterialTheme.colorScheme.primary', content)
    content = re.sub(r'Text\("Hapus",\s*color\s*=\s*Color\.White', 'Text("Hapus", color = MaterialTheme.colorScheme.onError', content)
    content = re.sub(r'Text\("Mulai Siklus Baru \(Reset\)",\s*color\s*=\s*Color\.White', 'Text("Mulai Siklus Baru (Reset)", color = MaterialTheme.colorScheme.onPrimary', content)
    
    # 4. Global replace for remaining Color.White
    content = content.replace('Color.White', 'MaterialTheme.colorScheme.onSurface')
    content = content.replace('TextSecondaryDark', 'MaterialTheme.colorScheme.onSurfaceVariant')
    content = content.replace('Color(0xFF3B82F6)', 'MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFF27272A)', 'MaterialTheme.colorScheme.surfaceVariant')
    content = content.replace('Color(0xFF2A2A30)', 'MaterialTheme.colorScheme.surfaceVariant')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
