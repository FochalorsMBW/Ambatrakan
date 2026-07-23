import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Replace usages
    content = content.replace('Color(0xFF27272A)', 'MaterialTheme.colorScheme.surfaceVariant')
    content = content.replace('Color(0xFF18181B)', 'MaterialTheme.colorScheme.background')
    content = content.replace('Color(0xFF3F3F46)', 'MaterialTheme.colorScheme.outlineVariant')
    content = content.replace('Color(0xFF60A5FA)', 'MaterialTheme.colorScheme.primary')
    content = content.replace('Color(0xFFA1A1AA)', 'MaterialTheme.colorScheme.onSurfaceVariant')
    content = content.replace('Color.White', 'MaterialTheme.colorScheme.onSurface')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
