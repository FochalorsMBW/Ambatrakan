import re
import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Refactor activeDeleteBillingItem
    del_bill_regex = r'AlertDialog\(\s*onDismissRequest\s*=\s*\{\s*activeDeleteBillingItem\s*=\s*null\s*\}.*?title\s*=\s*\{\s*Text\("Hapus Tagihan.*?confirmButton\s*=\s*\{.*?Button\(\s*onClick\s*=\s*\{\s*viewModel\.deleteBillingItem\(b\);\s*activeDeleteBillingItem\s*=\s*null\s*\}.*?\}\s*\},.*?dismissButton\s*=\s*\{.*?TextButton\(onClick\s*=\s*\{\s*activeDeleteBillingItem\s*=\s*null\s*\}\).*?\}\s*\)\s*'
    del_bill_replacement = """Dialog(onDismissRequest = { activeDeleteBillingItem = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Hapus Tagihan?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tagihan '${b.name}' akan dihapus secara permanen dari daftar tagihan.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeDeleteBillingItem = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.deleteBillingItem(b); activeDeleteBillingItem = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Hapus", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
"""
    content = re.sub(del_bill_regex, del_bill_replacement, content, flags=re.DOTALL)

    # Refactor activeEditBillingItem DatePickerDialog to Dialog
    datepicker_regex = r'DatePickerDialog\(\s*onDismissRequest\s*=\s*\{\s*showDatePicker\s*=\s*false\s*\},.*?confirmButton\s*=\s*\{.*?showDatePicker\s*=\s*false.*?\},.*?dismissButton\s*=\s*\{.*?showDatePicker\s*=\s*false.*?\},.*?DatePicker\([^)]*\)\s*\)\s*\}\s*\)'
    datepicker_replacement = """Dialog(onDismissRequest = { showDatePicker = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            DatePicker(
                                state = datePickerState,
                                colors = DatePickerDefaults.colors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                                    todayContentColor = MaterialTheme.colorScheme.primary,
                                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            val cal = java.util.Calendar.getInstance()
                                            cal.timeInMillis = it
                                            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                            cal.set(java.util.Calendar.MINUTE, 59)
                                            cal.set(java.util.Calendar.SECOND, 59)
                                            deadlineTimestamp = cal.timeInMillis
                                        }
                                        showDatePicker = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Pilih", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }"""
    content = re.sub(datepicker_regex, datepicker_replacement, content, flags=re.DOTALL)
    
    # Refactor activeEditBillingItem AlertDialog to Dialog
    edit_bill_regex = r'AlertDialog\(\s*onDismissRequest\s*=\s*\{\s*activeEditBillingItem\s*=\s*null\s*\}.*?confirmButton\s*=\s*\{.*?Button\(\s*onClick\s*=\s*\{\s*val\s*amt\s*=\s*editAmount\.toDoubleOrNull\(\).*?activeEditBillingItem\s*=\s*null.*?\}\s*\},.*?dismissButton\s*=\s*\{.*?TextButton\(onClick\s*=\s*\{\s*activeEditBillingItem\s*=\s*null\s*\}\).*?\}\s*\)\s*'
    edit_bill_replacement = """Dialog(onDismissRequest = { activeEditBillingItem = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        Text("Edit Tagihan", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        ManageTextField(value = editName, onValueChange = { editName = it }, placeholder = "Nama Tagihan")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ManageTextField(value = editAmount, onValueChange = { if(it.isEmpty() || it.all { char -> char.isDigit() }) editAmount = it }, placeholder = "Nominal")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Tipe Tagihan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            types.forEach { (type, desc) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (editType == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant).clickable { editType = type }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = (editType == type), onClick = { editType = type })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        if (editType != "GLOBAL") {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Batas Waktu (Opsional)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ManageTextField(
                                    value = deadlineTimestamp?.let { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "",
                                    onValueChange = {},
                                    placeholder = "Pilih Batas Waktu (Tanggal)"
                                )
                                Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { showDatePicker = true })
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    val newDeadline = deadlineTimestamp ?: (System.currentTimeMillis() + (24L * 60 * 60 * 1000))
                                    val updatedBill = b.copy(
                                        name = editName,
                                        amount = editAmount.toDoubleOrNull() ?: b.amount,
                                        billingType = editType,
                                        paidTenantIds = emptyList(),
                                        cycleStartTimestamp = System.currentTimeMillis(),
                                        deadlineTimestamp = newDeadline
                                    )
                                    viewModel.updateBillingItem(updatedBill)
                                    activeEditBillingItem = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Mulai Siklus Baru (Reset)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { activeEditBillingItem = null }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    val amt = editAmount.toDoubleOrNull()
                                    if (editName.isNotBlank() && amt != null) { 
                                        viewModel.updateBillingItem(b.copy(name = editName, amount = amt, billingType = editType))
                                        activeEditBillingItem = null 
                                    } 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text("Simpan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) 
                            } 
                        }
                    }
                }
            }
"""
    content = re.sub(edit_bill_regex, edit_bill_replacement, content, flags=re.DOTALL)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
