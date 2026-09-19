document.addEventListener('DOMContentLoaded', () => {
    const container = document.getElementById('project-details-container');
    const urlParams = new URLSearchParams(window.location.search);
    const projectId = urlParams.get('id');

    if (!projectId) {
        showError("No Project ID provided in URL.");
        return;
    }

    // Fetch all projects and find the matching one
    fetch('/MPLADs/api/projects')
        .then(res => res.json())
        .then(projects => {
            const p = projects.find(proj => String(proj.project_id) === String(projectId));
            
            if (p) {
                renderDetails(p);
            } else {
                showError("Project not found in database.");
            }
        })
        .catch(e => {
            console.error(e);
            showError("Failed to fetch project details from backend.");
        });

    function showError(msg) {
        container.innerHTML = `
            <div style="text-align: center; padding: 80px;">
                <h2 style="margin-bottom: 1rem;">Error</h2>
                <p style="margin-bottom: 2rem;">${msg}</p>
                <a href="projects.html" class="btn-ai" style="display: inline-block; width: auto; padding: 12px 30px;">← Back to Projects</a>
            </div>
        `;
    }

    function getStatusPillClass(status) {
        if (status === 'Completed') return 'completed';
        if (status === 'Ongoing' || status === 'In Progress') return 'in-progress';
        if (status === 'Unsanctioned') return 'unsanctioned';
        if (status === 'Delayed') return 'delayed';
        return 'in-progress';
    }

    function getStatusTextClass(status) {
        if (status === 'Completed') return 'green';
        if (status === 'Ongoing' || status === 'In Progress') return 'blue';
        if (status === 'Unsanctioned') return 'red';
        if (status === 'Delayed') return 'yellow';
        return 'blue';
    }

    function getRiskZone(status) {
        if (status === 'Unsanctioned') return { zone: 'RED ZONE', cls: 'red', score: 82, desc: 'Timeline and allocation pattern requires verification.' };
        if (status === 'Delayed') return { zone: 'YELLOW ZONE', cls: 'yellow', score: 55, desc: 'Moderate indicators detected. Further review recommended.' };
        return { zone: 'GREEN ZONE', cls: 'green', score: 18, desc: 'Lower risk indicators. Project proceeding normally.' };
    }

    function renderDetails(p) {
        const status = p.project_status || 'Ongoing';
        const pillClass = getStatusPillClass(status);
        const statusTextClass = getStatusTextClass(status);
        const risk = getRiskZone(status);
        const allocationCr = ((p.allocation_amount || 0) / 10000000).toFixed(2);

        container.innerHTML = `
            <!-- Header -->
            <div class="detail-header">
                <a href="projects.html" class="back-link">← Back to Projects</a>
                <div class="detail-status">
                    <span class="status-pill ${pillClass}">${status}</span>
                </div>
                <p class="project-id">Project #${p.project_id}</p>
                <h1>${p.work_ || 'Project Details'}</h1>
                <p class="breadcrumb">${p.state || 'N/A'}<span>•</span>${p.constituency || 'N/A'}<span>•</span>${p.house || 'General'}</p>
            </div>

            <!-- Main Grid -->
            <div class="detail-grid">
                <!-- PROJECT OVERVIEW -->
                <div class="info-card">
                    <h3>Project Overview</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">MP Name</div>
                            <div class="info-value">${p.mp_name || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Category</div>
                            <div class="info-value">${p.house || 'General'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">State</div>
                            <div class="info-value">${p.state || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Constituency</div>
                            <div class="info-value">${p.constituency || 'N/A'}</div>
                        </div>
                    </div>
                </div>

                <!-- LOCATION -->
                <div class="info-card">
                    <h3>Location</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">State</div>
                            <div class="info-value">${p.state || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Constituency</div>
                            <div class="info-value">${p.constituency || 'N/A'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">IDA</div>
                            <div class="info-value">${p.ida || 'N/A'}</div>
                        </div>
                    </div>
                </div>

                <!-- AI RISK ASSESSMENT -->
                <div class="ai-card">
                    <h3>AI Risk Assessment</h3>
                    <div class="risk-score">${risk.score}<small> / 100</small></div>
                    <div class="risk-zone ${risk.cls}"><span class="dot"></span> ${risk.zone}</div>
                    <div class="risk-label">Potential Risk Indicator</div>
                    <div class="risk-desc">${risk.desc}</div>
                    <div class="risk-meta">
                        <b>Prototype AI assessment</b>
                        Generated today, ${new Date().toLocaleTimeString('en-IN', {hour: '2-digit', minute: '2-digit', hour12: true})}
                    </div>
                    <button class="btn-ai" onclick="runAnalysis(${p.project_id})">Run AI Analysis →</button>
                </div>
            </div>

            <!-- Bottom Row -->
            <div class="detail-bottom">
                <!-- FINANCIAL INFORMATION -->
                <div class="info-card">
                    <h3>Financial Information</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Allocation Amount</div>
                            <div class="info-value">₹${allocationCr} Cr</div>
                        </div>
                        <div>
                            <div class="info-label">Project ID</div>
                            <div class="info-value">${p.project_id}</div>
                        </div>
                    </div>
                </div>

                <!-- APPROVAL & STATUS -->
                <div class="info-card">
                    <h3>Approval & Status</h3>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Date</div>
                            <div class="info-value">${p.Date_ || 'N/A'}</div>
                        </div>
                        <div>
                            <div class="info-label">Approval</div>
                            <div class="info-value">${p.ida_approval || 'N/A'}</div>
                        </div>
                    </div>
                    <div class="info-row">
                        <div>
                            <div class="info-label">Status</div>
                            <div class="info-value status-text ${statusTextClass}">${status}</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
});

// AI Analysis trigger
function runAnalysis(projectId) {
    const btn = document.querySelector('.btn-ai');
    if (btn) btn.textContent = 'Analyzing...';

    fetch('/MPLADs/api/analyze?id=' + projectId)
        .then(r => r.json())
        .then(analysis => {
            if (btn) btn.textContent = '✓ Analysis Complete';
            alert('AI Analysis Complete!\n\nRisk Score: ' + (analysis.risk_score || 'N/A') + '\nRecommendation: ' + (analysis.recommendation || 'No anomalies detected.'));
        })
        .catch(e => {
            console.log('ML Service unavailable:', e);
            if (btn) btn.textContent = 'Run AI Analysis →';
            alert('ML Service is not running. Start the Flask server first.');
        });
}
