import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the line: Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    # right before types.forEach { (type, desc) ->
    old_code = r'Column\(verticalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{\s*types\.forEach \{ \(type, desc\) ->'
    new_code = r'''val editTypes = listOf("ROLLING" to "Giliran (cth: Galon)", "DEMAND" to "Kapan Saja (cth: Token)", "SHARED" to "Patungan (cth: Wi-Fi)", "GLOBAL" to "Tetap (Semua Penghuni)")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            editTypes.forEach { (type, desc) ->'''
    
    # We only want to replace it inside the dialog.
    # The dialog's code has Text("Tipe Tagihan" ... 
    content = re.sub(old_code, new_code, content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
