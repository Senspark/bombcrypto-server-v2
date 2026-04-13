// Utility to open the example Excel demo in a new tab

export function openExampleExcelDemo() {
    const html = `<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>Example Data</title><style>
        body{font-family:sans-serif;padding:24px;}
        table{border-collapse:collapse;width:100%;max-width:600px;}
        th,td{border:1px solid #ccc;padding:8px;text-align:left;}
        th{background:#f5f5f5;}
        tr.example-row td {background:#e6f7ff; border:2px solid #1890ff;}
        tr.new-row td {background:#f6ffed; border:1px solid #b7eb8f;}
        tr:nth-child(even):not(.example-row):not(.new-row){background:#fafafa;}
        caption{font-size:1.2em;margin-bottom:8px;font-weight:bold;}
        .arrow {color:#1890ff;font-size:1.2em;vertical-align:middle;margin-right:4px;}
        input[type=text]{border:none;background:transparent;width:100%;font-family:sans-serif;font-size:1em;outline:none;}
        input[type=text]:focus{background:#fffbe6;}
        .add-btn{
            display:inline-flex;align-items:center;gap:6px;
            margin-top:14px;padding:7px 18px;
            background:#1890ff;color:#fff;border:none;border-radius:5px;
            font-size:1em;cursor:pointer;transition:background 0.2s;
        }
        .add-btn:hover{background:#096dd9;}
        .add-btn:disabled{background:#aaa;cursor:not-allowed;}
        .add-btn svg{vertical-align:middle;}
        .row-count{margin-left:12px;color:#888;font-size:0.93em;vertical-align:middle;}
    </style></head><body>
    <div style='margin-bottom:12px;font-weight:500;'>
        <span style='color:#1890ff;'>Copy only the highlighted rows below (do not include the header):</span>
    </div>
    <table id='demo-table'>
        <thead>
            <tr><th>Wallet Address 1</th><th>Time</th><th>Wallet Address 2</th></tr>
        </thead>
        <tbody id='demo-tbody'>
            <tr class='example-row'><td>wallet address 1</td><td>9:00 - 9:12</td><td>wallet address 2</td></tr>
            <tr class='example-row'><td>wallet address 3</td><td>9:00 - 9:12</td><td>wallet address 4</td></tr>
        </tbody>
    </table>
    <div style='margin-top:4px;'>
        <button class='add-btn' id='add-row-btn' onclick='addRow()'>
            <svg width='16' height='16' viewBox='0 0 16 16' fill='none' xmlns='http://www.w3.org/2000/svg'>
                <circle cx='8' cy='8' r='7.5' stroke='white' stroke-width='1.2'/>
                <line x1='8' y1='4.5' x2='8' y2='11.5' stroke='white' stroke-width='1.8' stroke-linecap='round'/>
                <line x1='4.5' y1='8' x2='11.5' y2='8' stroke='white' stroke-width='1.8' stroke-linecap='round'/>
            </svg>
            Add Row
        </button>
        <span class='row-count' id='row-count'>2 / 20</span>
    </div>
    <div style='margin-top:14px;color:#888;font-size:0.95em;'>
        <span class='arrow'>&#8592;</span> Select and copy only these rows as tab-separated values for pasting into the input.
    </div>
    <script>
        const MAX_ROWS = 20;
        function getRowCount() {
            return document.getElementById('demo-tbody').rows.length;
        }
        function updateUI() {
            const count = getRowCount();
            document.getElementById('row-count').textContent = count + ' / ' + MAX_ROWS;
            document.getElementById('add-row-btn').disabled = count >= MAX_ROWS;
        }
        function addRow() {
            const tbody = document.getElementById('demo-tbody');
            if (tbody.rows.length >= MAX_ROWS) return;
            const tr = document.createElement('tr');
            tr.className = 'new-row';
            ['wallet address ...', '9:00 - 9:12', 'wallet address ...'].forEach(function(placeholder) {
                const td = document.createElement('td');
                const input = document.createElement('input');
                input.type = 'text';
                input.placeholder = placeholder;
                td.appendChild(input);
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
            updateUI();
            tr.querySelector('input').focus();
        }
        updateUI();
    </script>
    </body></html>`;
    const blob = new Blob([html], {type: 'text/html'});
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
}
