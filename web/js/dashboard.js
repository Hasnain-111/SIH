document.addEventListener('DOMContentLoaded', () => {
    const tbody = document.querySelector('#recent-projects-table tbody');
    
    // Fetch recent projects from Java backend
    if (tbody) {
        fetch('/MPLADs/api/projects')
            .then(res => res.json())
            .then(projects => {
                // Update total stats safely
                try {
                    const elProjects = document.getElementById('total-projects');
                    if (elProjects) elProjects.textContent = projects.length.toLocaleString();
                    
                    const elDonutTotal = document.getElementById('donut-total-projects');
                    if (elDonutTotal) elDonutTotal.textContent = projects.length.toLocaleString();
                    
                    // Zone counts based on project_status
                    const redCount = projects.filter(p => p.project_status === 'Unsanctioned').length;
                    const yellowCount = projects.filter(p => p.project_status === 'Delayed').length;
                    const greenCount = projects.filter(p => p.project_status === 'Completed' || p.project_status === 'Ongoing').length;
                    
                    // KPI Cards
                    const elRed = document.getElementById('kpi-red-zone');
                    if (elRed) elRed.textContent = redCount.toLocaleString();
                    
                    const elYellow = document.getElementById('kpi-yellow-zone');
                    if (elYellow) elYellow.textContent = yellowCount.toLocaleString();
                    
                    const elGreen = document.getElementById('kpi-green-zone');
                    if (elGreen) elGreen.textContent = greenCount.toLocaleString();
                    
                    // Donut legend
                    const redPct = projects.length ? ((redCount / projects.length) * 100).toFixed(1) : 0;
                    const yellowPct = projects.length ? ((yellowCount / projects.length) * 100).toFixed(1) : 0;
                    const greenPct = projects.length ? ((greenCount / projects.length) * 100).toFixed(1) : 0;
                    
                    if (document.getElementById('donut-red')) document.getElementById('donut-red').textContent = redCount + ' (' + redPct + '%)';
                    if (document.getElementById('donut-yellow')) document.getElementById('donut-yellow').textContent = yellowCount + ' (' + yellowPct + '%)';
                    if (document.getElementById('donut-green')) document.getElementById('donut-green').textContent = greenCount + ' (' + greenPct + '%)';
                    
                } catch (err) {
                    console.error("Error calculating stats:", err);
                }
                
                // Priority Review Queue shows ONE row for records that share the same
                // MP name + work + allocation amount (lowest project_id is kept).
                // KPI cards / donut above still use ALL projects, so totals do not change.
                const norm = v => String(v == null ? '' : v).trim();
                const seen = new Set();
                const uniqueProjects = projects
                    .slice()
                    .sort((a, b) => Number(a.project_id) - Number(b.project_id))
                    .filter(p => {
                        const key = JSON.stringify([norm(p.mp_name), norm(p.work_), p.allocation_amount]);
                        if (seen.has(key)) return false;
                        seen.add(key);
                        return true;
                    });

                // Pagination Logic (30 per page)
                const itemsPerPage = 30;
                let currentPage = 1;
                
                function displayPage(page) {
                    tbody.innerHTML = '';
                    const start = (page - 1) * itemsPerPage;
                    const end = start + itemsPerPage;
                    const paginatedItems = uniqueProjects.slice(start, end);
                    
                    paginatedItems.forEach(project => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><a href="project-details.html?id=${project.project_id}" style="color: var(--accent-color); text-decoration: none;">${project.work_ || 'N/A'}</a></td>
                            <td>${project.mp_name || 'N/A'}</td>
                            <td>${project.constituency || 'N/A'}</td>
                            <td>₹${(project.allocation_amount || 0).toLocaleString('en-IN')}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                    
                    const totalPages = Math.ceil(uniqueProjects.length / itemsPerPage) || 1;
                    
                    // Add Improved Pagination Controls
                    const paginationRow = document.createElement('tr');
                    paginationRow.innerHTML = `
                        <td colspan="4" style="text-align: center; padding: 25px;">
                            <div style="display:inline-flex; align-items:center; gap: 20px; background: rgba(255,255,255,0.03); padding: 10px 24px; border-radius: 30px; border: 1px solid var(--border-light);">
                                <button id="prevBtn" ${page === 1 ? 'disabled' : ''} style="background:transparent; border:none; color: ${page === 1 ? '#4b5563' : '#f3f4f6'}; cursor: ${page === 1 ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">← Previous</button>
                                <span style="font-weight: 600; color: #d4af37; font-size: 0.95rem; padding: 0 10px;">Page ${page} of ${totalPages}</span>
                                <button id="nextBtn" ${end >= uniqueProjects.length ? 'disabled' : ''} style="background:transparent; border:none; color: ${end >= uniqueProjects.length ? '#4b5563' : '#f3f4f6'}; cursor: ${end >= uniqueProjects.length ? 'not-allowed' : 'pointer'}; font-weight:600; font-size: 0.9rem; transition: color 0.2s;">Next →</button>
                            </div>
                        </td>
                    `;
                    tbody.appendChild(paginationRow);
                    
                    if(document.getElementById('prevBtn')) {
                        document.getElementById('prevBtn').addEventListener('click', () => { currentPage--; displayPage(currentPage); });
                    }
                    if(document.getElementById('nextBtn')) {
                        document.getElementById('nextBtn').addEventListener('click', () => { currentPage++; displayPage(currentPage); });
                    }
                }
                
                displayPage(currentPage);
            })
            .catch(e => {
                console.error("Failed to load dashboard data:", e);
                tbody.innerHTML = '<tr><td colspan="4" style="color:red;">Error loading data from backend</td></tr>';
            });
    }
});