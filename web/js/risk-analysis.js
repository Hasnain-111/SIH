document.addEventListener('DOMContentLoaded', () => {
    const riskData = [
        {
            projectId: 3,
            name: 'Upgradation of Primary Health Centre',
            level: 'High',
            reason: 'Funding delayed by 3 months. Contractor reported material shortage.',
            predictedDelay: '4-6 months',
            financialImpact: '₹2,00,000 cost overrun expected'
        },
        {
            projectId: 5,
            name: 'Panchayat Road Paving',
            level: 'Medium',
            reason: 'Monsoon season approaching, 70% earthwork still pending.',
            predictedDelay: '2 months',
            financialImpact: 'Minimal at current stage'
        }
    ];

    const container = document.getElementById('risk-container');

    function renderRisks() {
        container.innerHTML = '';
        riskData.forEach(risk => {
            const card = document.createElement('div');
            const isHigh = risk.level === 'High';
            card.className = `risk-card ${isHigh ? '' : 'medium-risk'}`;
            
            card.innerHTML = `
                <span class="risk-badge ${isHigh ? 'badge-high' : 'badge-medium'}">${risk.level} Risk</span>
                <h3><a href="project-details.html?id=${risk.projectId}" style="color: inherit; text-decoration: none;">${risk.name}</a></h3>
                <div class="risk-details">
                    <p><strong>Reason:</strong> ${risk.reason}</p>
                    <p><strong>Predicted Delay:</strong> ${risk.predictedDelay}</p>
                    <p><strong>Financial Impact:</strong> ${risk.financialImpact}</p>
                </div>
            `;
            container.appendChild(card);
        });
    }

    renderRisks();

    document.getElementById('btn-refresh').addEventListener('click', () => {
        const btn = document.getElementById('btn-refresh');
        btn.textContent = 'Refreshing...';
        btn.disabled = true;
        
        setTimeout(() => {
            btn.textContent = 'Refresh Data';
            btn.disabled = false;
            // Re-render to simulate refresh
            renderRisks();
            alert('Risk data successfully refreshed from AI engine.');
        }, 800);
    });
});
