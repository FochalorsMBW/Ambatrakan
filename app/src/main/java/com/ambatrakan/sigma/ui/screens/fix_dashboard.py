import sys

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Add allTenants to DashboardScreen
    if 'val allTenants by viewModel.allTenants.collectAsState()' not in content:
        content = content.replace(
            'val tenants by viewModel.tenants.collectAsState()',
            'val tenants by viewModel.tenants.collectAsState()\n    val allTenants by viewModel.allTenants.collectAsState()'
        )

    # 2. Fix admin filteredBillingItems
    content = content.replace(
        'val filteredBillingItems = billingItems.filter { it.locationId == activeLocationId || activeLocationId == null }',
        'val filteredBillingItems = billingItems.filter { it.locationId == activeLocationId || it.locationId == null }'
    )

    # 3. Update MyPendingBillsSection signature and call
    content = content.replace(
        'fun MyPendingBillsSection(\n    myBills: List<BillingItem>, \n    locations: List<Location>, \n    tenants: List<Tenant>,',
        'fun MyPendingBillsSection(\n    myBills: List<BillingItem>, \n    locations: List<Location>, \n    tenants: List<Tenant>,\n    allTenants: List<Tenant>,'
    )
    
    content = content.replace(
        '                tenants = tenants,\n                currentTenantId = currentTenant?.id,',
        '                tenants = tenants,\n                allTenants = allTenants,\n                currentTenantId = currentTenant?.id,'
    )

    # 4. Pass allTenants to GlobalBillCard
    content = content.replace(
        '                        allLogs = allLogs,\n                        tenants = tenants,\n                        onPay = onPay\n                    )',
        '                        allLogs = allLogs,\n                        tenants = allTenants,\n                        onPay = onPay\n                    )'
    )
    
    # 5. Fix Admin Pending Approvals
    # For Admin Pending Approvals, we might also need allTenants if they approve global bills.
    # Wait, admin pending approvals uses allLogs.tenantName so it might be fine, but let's check if it uses tenants.
    
    # 6. GlobalBillCard signature doesn't need to change if it just expects a list of tenants. We pass `allTenants` instead of `tenants`.
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    process_file(sys.argv[1])
