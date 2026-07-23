import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Add countdown formatter function at the top level
    func = """
fun formatDeadlineWithCountdown(deadlineMs: Long): String {
    val diff = deadlineMs - System.currentTimeMillis()
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()
    val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date(deadlineMs))
    return if (days > 0) {
        "$dateStr ($days Hari Lagi)"
    } else if (days == 0) {
        "$dateStr (Hari Ini)"
    } else {
        "$dateStr (Terlewat)"
    }
}
"""
    if "formatDeadlineWithCountdown" not in content:
        # insert after imports
        content = re.sub(r'(import .*?\n\n)', r'\1' + func, content, count=1)
        
    # 2. Replace the ManageTextField value for deadline
    old_val = r'value\s*=\s*deadlineTimestamp\?\.let\s*\{\s*java\.text\.SimpleDateFormat\("dd\s*MMM\s*yyyy",\s*java\.util\.Locale\.getDefault\(\)\)\.format\(java\.util\.Date\(it\)\)\s*\}\s*\?:\s*""'
    new_val = 'value = deadlineTimestamp?.let { formatDeadlineWithCountdown(it) } ?: ""'
    content = re.sub(old_val, new_val, content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
