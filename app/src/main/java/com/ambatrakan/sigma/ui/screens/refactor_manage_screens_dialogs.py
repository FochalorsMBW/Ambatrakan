import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Refactor activeEditLocation
    edit_loc_regex = r'AlertDialog\(\s*onDismissRequest\s*=\s*\{\s*activeEditLocation\s*=\s*null\s*\}.*?confirmButton\s*=\s*\{.*?Button\(\s*onClick\s*=\s*\{\s*if\s*\(editLocName\.isNotBlank\(\)\)\s*\{\s*viewModel\.updateLocation\(location\.copy\(name\s*=\s*editLocName\)\);\s*activeEditLocation\s*=\s*null\s*\}\s*\}.*?\}\s*\},.*?dismissButton\s*=\s*\{.*?TextButton\(onClick\s*=\s*\{\s*activeEditLocation\s*=\s*null\s*\}\).*?\}\s*\)\s*'
    
    edit_loc_replacement = """Dialog(onDismissRequest = { activeEditLocation = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Edit Nama Lokasi", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        ManageTextField(value = editLocName, onValueChange = { editLocName = it }, placeholder = "Nama Lokasi")
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeEditLocation = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { if (editLocName.isNotBlank()) { viewModel.updateLocation(location.copy(name = editLocName)); activeEditLocation = null } },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Simpan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
"""
    content = re.sub(edit_loc_regex, edit_loc_replacement, content, flags=re.DOTALL)
    
    # 2. Refactor activeDeleteLocation
    del_loc_regex = r'AlertDialog\(\s*onDismissRequest\s*=\s*\{\s*activeDeleteLocation\s*=\s*null\s*\}.*?title\s*=\s*\{\s*Text\("Hapus Lokasi.*?confirmButton\s*=\s*\{.*?Button\(\s*onClick\s*=\s*\{\s*viewModel\.deleteLocation\(location\);\s*activeDeleteLocation\s*=\s*null\s*\}.*?\}\s*\},.*?dismissButton\s*=\s*\{.*?TextButton\(onClick\s*=\s*\{\s*activeDeleteLocation\s*=\s*null\s*\}\).*?\}\s*\)\s*'
    
    del_loc_replacement = """Dialog(onDismissRequest = { activeDeleteLocation = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Hapus Lokasi?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Lokasi '${location.name}' dan semua penghuni serta tagihan di dalamnya akan dihapus secara permanen.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeDeleteLocation = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.deleteLocation(location); activeDeleteLocation = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Hapus", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
"""
    content = re.sub(del_loc_regex, del_loc_replacement, content, flags=re.DOTALL)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
