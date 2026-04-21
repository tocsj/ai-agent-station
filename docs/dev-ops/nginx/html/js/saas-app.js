/**
 * AI Agent Station - Platform Application Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    loadDashboard(); // Default view
});

// Navigation State
let currentView = 'dashboard';

function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');
            
            const target = item.getAttribute('data-target');
            document.getElementById('top-breadcrumb').textContent = `主控制台 / ${item.querySelector('span').textContent}`;
            switchView(target);
        });
    });
}

function switchView(viewName) {
    currentView = viewName;
    const container = document.getElementById('view-container');
    container.innerHTML = ''; // Clear current
    
    switch(viewName) {
        case 'dashboard':
            container.innerHTML = renderDashboard();
            break;
        case 'resume-agent':
            container.innerHTML = renderResumeAgent();
            initResumeAgent();
            break;
        case 'mock-interview':
            container.innerHTML = renderMockInterview();
            initMockInterview();
            break;
        case 'doc-assistant':
            container.innerHTML = renderMockPage('文档知识助手', 'fas fa-book', '正在构建强大的文档检索和对比功能，敬请期待。');
            break;
        case 'content-auto':
            container.innerHTML = renderMockPage('内容自动化', 'fas fa-pen-nib', '编排您的多平台内容发布工作流。');
            break;
        case 'agent-runtime':
            container.innerHTML = renderMockPage('Agent 运行详情', 'fas fa-microchip', '深度可观测的节点链路执行详情。');
            break;
        case 'audit-monitor':
            container.innerHTML = renderMockPage('审计与监控', 'fas fa-shield-halved', '企业级权限控制、成本概览及全量调用日志。');
            break;
    }
}

// ----------------------
// 1. Dashboard
// ----------------------
function renderDashboard() {
    return `
        <div class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-header"><span>总任务数</span><div class="kpi-icon"><i class="fas fa-layer-group"></i></div></div>
                <div class="kpi-value">12,483</div>
                <div class="kpi-trend up"><i class="fas fa-arrow-up"></i> 12.5% 较上周</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-header"><span>平均响应时间 (ms)</span><div class="kpi-icon"><i class="fas fa-stopwatch"></i></div></div>
                <div class="kpi-value">845</div>
                <div class="kpi-trend down"><i class="fas fa-arrow-down"></i> 4.2% 较上周</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-header"><span>Tool 调用次数</span><div class="kpi-icon"><i class="fas fa-wrench"></i></div></div>
                <div class="kpi-value">85,201</div>
                <div class="kpi-trend up"><i class="fas fa-arrow-up"></i> 22.1% 较上周</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-header"><span>平台成功率</span><div class="kpi-icon"><i class="fas fa-check-circle"></i></div></div>
                <div class="kpi-value">99.2%</div>
                <div class="kpi-trend up"><i class="fas fa-arrow-up"></i> 0.1% 较上周</div>
            </div>
        </div>
        
        <div class="card mb-4">
            <div class="card-header">
                <div class="card-title"><i class="fas fa-list"></i> 最近 Agent 运行记录</div>
                <button class="btn btn-outline btn-sm">查看全部</button>
            </div>
            <div class="card-body" style="padding: 0;">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>任务 ID</th>
                            <th>流程类型</th>
                            <th>状态</th>
                            <th>运行时长</th>
                            <th>Token 消耗</th>
                            <th>发生时间</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="text-mono">#TSK-20260413-8991</td>
                            <td>简历评估 (Java Backend)</td>
                            <td><span class="status-badge status-success">Completed</span></td>
                            <td>3.2s</td>
                            <td class="text-mono">4,201</td>
                            <td>2 分钟前</td>
                        </tr>
                        <tr>
                            <td class="text-mono">#TSK-20260413-8990</td>
                            <td>模拟面试 (轮次 2/5)</td>
                            <td><span class="status-badge status-running">Thinking</span></td>
                            <td>-</td>
                            <td class="text-mono">-</td>
                            <td>刚刚</td>
                        </tr>
                        <tr>
                            <td class="text-mono">#TSK-20260413-8989</td>
                            <td>文档问答 (架构组)</td>
                            <td><span class="status-badge status-success">Completed</span></td>
                            <td>1.8s</td>
                            <td class="text-mono">1,844</td>
                            <td>15 分钟前</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// ----------------------
// 2. Resume Agent
// ----------------------
function renderResumeAgent() {
    return `
        <div class="page-layout-3col">
            <!-- Left: Config & Context -->
            <div class="col-left">
                <div class="card">
                    <div class="card-header"><div class="card-title"><i class="fas fa-upload"></i> 上传并提取上下文</div></div>
                    <div class="card-body">
                        <div id="resume-upload-zone" class="file-upload-zone mb-4" onclick="handleMockResumeUpload()">
                            <div class="file-upload-icon"><i class="fas fa-file-pdf"></i></div>
                            <div class="file-upload-text">点击上传简历 PDF</div>
                            <div class="file-upload-hint">支持 .pdf，最大 10MB</div>
                        </div>
                        
                        <div id="resume-context-card" class="context-card hidden mb-4">
                            <div class="context-header">
                                <i class="fas fa-file-pdf"></i>
                                <div class="context-title" id="ctx-filename">...</div>
                                <span class="tag tag-success">已向量化</span>
                            </div>
                            <div class="context-meta">
                                <div class="meta-item"><span class="meta-label">Resume ID</span><span class="meta-value" id="ctx-id">-</span></div>
                                <div class="meta-item"><span class="meta-label">Knowledge Space</span><span class="meta-value" id="ctx-space">-</span></div>
                                <div class="meta-item"><span class="meta-label">Chunk Count</span><span class="meta-value" id="ctx-chunk">-</span></div>
                                <div class="meta-item"><span class="meta-label">Tokens</span><span class="meta-value">~3,400</span></div>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label">选择目标岗位标准</label>
                            <select class="form-select" id="target-job-select">
                                <option value="java_senior">高级 Java 开发工程师</option>
                                <option value="ai_agent">AI Agent 工程师</option>
                                <option value="arch">系统架构师</option>
                            </select>
                        </div>
                        
                        <button id="btn-start-eval" class="btn btn-primary btn-block mb-2" disabled onclick="startResumeEval()"><i class="fas fa-magic"></i> 开始深度评估</button>
                        <button id="btn-go-interview" class="btn btn-outline btn-block" disabled onclick="goMockInterview()"><i class="fas fa-user-tie"></i> 进入模拟面试</button>
                    </div>
                </div>
            </div>

            <!-- Middle: Main Results -->
            <div class="col-main">
                <div class="card h-full" style="height: 100%;">
                    <div class="card-header">
                        <div class="card-title"><i class="fas fa-chart-pie"></i> 评估结果报告</div>
                        <div class="flex-row">
                            <span id="eval-status" class="status-badge hidden">...</span>
                        </div>
                    </div>
                    <div class="card-body custom-scrollbar" id="eval-result-area">
                        <div class="empty-state">
                            <i class="fas fa-inbox empty-icon"></i>
                            <div class="empty-title">等待评估</div>
                            <div class="empty-desc">请先在左侧上传简历并点击"开始深度评估"</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right: Agent Workflow Timeline -->
            <div class="col-right">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title"><i class="fas fa-network-wired"></i> Agent 执行链路</div>
                    </div>
                    <div class="card-body">
                        <div class="timeline" id="agent-timeline">
                            <div class="empty-state" style="padding: 20px;">暂无日志</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function initResumeAgent() {
    // Initialized context.
}

function handleMockResumeUpload() {
    const zone = document.getElementById('resume-upload-zone');
    zone.innerHTML = `<div class="file-upload-icon" style="color: var(--text-muted);"><i class="fas fa-circle-notch fa-spin"></i></div><div class="file-upload-text">正在处理并存入向量库...</div>`;
    
    // Mock /api/v1/resume/upload
    setTimeout(() => {
        const mockResponse = {
            "code": "0000",
            "info": "success",
            "data": {
                "resumeId": 7,
                "knowledgeSpaceId": 7,
                "knowledgeTag": "resume_knowledge_space",
                "fileName": "test_resume.pdf",
                "chunkCount": 12
            }
        };
        
        zone.classList.add('hidden');
        const ctxCard = document.getElementById('resume-context-card');
        ctxCard.classList.remove('hidden');
        
        document.getElementById('ctx-filename').textContent = mockResponse.data.fileName;
        document.getElementById('ctx-id').textContent = mockResponse.data.resumeId;
        document.getElementById('ctx-space').textContent = mockResponse.data.knowledgeSpaceId;
        document.getElementById('ctx-chunk').textContent = mockResponse.data.chunkCount;
        
        document.getElementById('btn-start-eval').disabled = false;
        
    }, 1500);
}

function startResumeEval() {
    document.getElementById('btn-start-eval').disabled = true;
    document.getElementById('eval-status').classList.remove('hidden');
    document.getElementById('eval-status').className = 'status-badge status-running';
    document.getElementById('eval-status').textContent = 'Agent 运行中...';
    
    const resultArea = document.getElementById('eval-result-area');
    resultArea.innerHTML = `
        <div class="results-grid">
            <div class="result-card full-width">
                 <div class="result-header"><div class="result-title"><i class="fas fa-star text-warning"></i> 综合评分预估</div></div>
                 <div class="result-value" id="r-score">-- / 100</div>
                 <p style="font-size: 12px; color: var(--text-muted); margin-top: 4px;" id="r-match">...</p>
            </div>
            <div class="result-card">
                 <div class="result-header"><div class="result-title"><i class="fas fa-plus-circle text-success"></i> 核心优势分析</div></div>
                 <ul class="result-list text-success" id="r-adv"></ul>
            </div>
            <div class="result-card">
                 <div class="result-header"><div class="result-title"><i class="fas fa-exclamation-triangle text-danger"></i> 潜在风险点</div></div>
                 <ul class="result-list text-danger" id="r-risk"></ul>
            </div>
            <div class="result-card full-width">
                 <div class="result-header"><div class="result-title"><i class="fas fa-lightbulb text-primary"></i> 总体调整建议</div></div>
                 <ul class="result-list" id="r-suggest"></ul>
            </div>
             <div class="result-card full-width">
                 <div class="result-header"><div class="result-title"><i class="fas fa-microscope text-primary"></i> 技术深度与项目详评</div></div>
                 <div class="md-content" id="r-deep"></div>
            </div>
        </div>
    `;
    
    const timeline = document.getElementById('agent-timeline');
    timeline.innerHTML = '';
    
    // Mock SSE Stream `/api/v1/resume/evaluate/stream`
    const events = [
        { type: "analysis", subType: "analysis_strategy", content: "解析目标岗位要求，开始执行向量检索策略。提取技能树：Java/Spring/Microservices/AI Agent。" },
        { type: "execution", subType: "execution_process", content: "命中 8 个简历 Chunk，包含项目经验和技术栈。" },
        { type: "execution", subType: "evaluation", content: "分析技术深度：具备主流微服务框架使用经验，AI Agent 项目真实性较高，存在 RAG 等关键节点实现。" },
        { type: "execution", subType: "score", data: { score: 85, match: "岗位匹配度较高，具备完整的相关项目经验" } },
        { type: "supervision", subType: "assessment", content: "复核评估输出：评分客观，论据完整。未发现幻觉和不兼容点。" },
        { type: "summary", subType: "summary_overview", data: {
            adv: ["Java 后端基础扎实", "具备前沿 AI Agent 落地经验", "复杂流式接口开发经验"],
            risk: ["高并发治理场景着墨较少", "缺乏大规模集群监控经验"],
            suggest: ["补充微服务性能优化指标", "在 Agent 项目中强调 RAG 检索命中率提升的手段"],
            deep: "<p>候选人的 <strong>AI Agent Station</strong> 项目体现了良好的系统工程能力。不仅实现了基础大模型对话，还抽象了 <code>Toolset</code> 和 <code>SSE 流式节点</code>，展现了企业级落地思维。</p>"
        }},
        { type: "complete", subType: "", content: "Agent 任务执行完毕" }
    ];
    
    let delay = 0;
    
    events.forEach((ev, idx) => {
        delay += Math.random() * 800 + 400; // 400-1200ms random delay
        setTimeout(() => {
            appendTimelineNode(timeline, ev);
            
            // Render results based on events
            if(ev.subType === 'score') {
                document.getElementById('r-score').innerHTML = `${ev.data.score} <span style="font-size: 14px; font-weight: 500; color:var(--text-muted)">/ 100</span>`;
                document.getElementById('r-match').textContent = ev.data.match;
            }
            if(ev.subType === 'summary_overview') {
                document.getElementById('r-adv').innerHTML = ev.data.adv.map(i => `<li><i class="fas fa-check"></i> ${i}</li>`).join('');
                document.getElementById('r-risk').innerHTML = ev.data.risk.map(i => `<li><i class="fas fa-xmark"></i> ${i}</li>`).join('');
                document.getElementById('r-suggest').innerHTML = ev.data.suggest.map(i => `<li><i class="fas fa-angle-right"></i> ${i}</li>`).join('');
                document.getElementById('r-deep').innerHTML = ev.data.deep;
            }
            
            if(ev.type === 'complete') {
                document.getElementById('eval-status').className = 'status-badge status-success';
                document.getElementById('eval-status').textContent = '评估完成';
                document.getElementById('btn-go-interview').disabled = false;
            }
        }, delay);
    });
}

function appendTimelineNode(container, ev) {
    const icons = {
        'analysis': 'fas fa-search',
        'execution': 'fas fa-bolt',
        'supervision': 'fas fa-gavel',
        'summary': 'fas fa-receipt',
        'complete': 'fas fa-check'
    };
    
    const colors = {
        'analysis': 'color: #2563eb;',
        'execution': 'color: #9333ea;',
        'supervision': 'color: #f59e0b;',
        'summary': 'color: #10b981;',
        'complete': 'color: #10b981;'
    };
    
    // Find if the same main type node exists
    let mainNode = document.getElementById(`tl-node-${ev.type}`);
    
    if(!mainNode) {
        mainNode = document.createElement('div');
        mainNode.className = 'timeline-item active';
        mainNode.id = `tl-node-${ev.type}`;
        
        mainNode.innerHTML = `
            <div class="timeline-icon" style="${colors[ev.type]}"><i class="${icons[ev.type] || 'fas fa-circle'}"></i></div>
            <div class="timeline-content">
                <div class="timeline-header">
                    <span class="timeline-title">${ev.type}</span>
                    <span class="timeline-time">${new Date().toLocaleTimeString()}</span>
                </div>
                <div class="timeline-body" id="tl-body-${ev.type}"></div>
            </div>
        `;
        container.appendChild(mainNode);
        
        // Deactivate previous
        const items = container.querySelectorAll('.timeline-item');
        if(items.length > 1) {
            items[items.length - 2].classList.remove('active');
            items[items.length - 2].classList.add('completed');
        }
    }
    
    // Append log step
    if(ev.content || ev.subType) {
         const body = document.getElementById(`tl-body-${ev.type}`);
         let logHtml = `<div class="log-entry">`;
         if(ev.subType) logHtml += `<div class="log-sub"><span class="log-badge">${ev.subType}</span><span>${ev.content || 'Processing data nodes...'}</span></div>`;
         logHtml += `</div>`;
         body.insertAdjacentHTML('beforeend', logHtml);
    }
    
    if(ev.type === 'complete') {
        const items = container.querySelectorAll('.timeline-item');
        items.forEach(i => { i.classList.remove('active'); i.classList.add('completed'); });
    }
    
    // auto scroll timeline container
    container.parentElement.scrollTop = container.parentElement.scrollHeight;
}

function goMockInterview() {
    switchView('mock-interview');
}

// ----------------------
// 3. Mock Interview
// ----------------------
let currentRound = 1;
const MAX_ROUNDS = 3;

function renderMockInterview() {
    return `
        <div class="page-layout-2col">
            <!-- Main Chat Area -->
            <div class="col-wide">
                <div class="card h-full">
                    <div class="card-header">
                        <div class="card-title">
                            <i class="fas fa-user-tie"></i> 模拟面试工作台 
                            <span class="tag tag-success" style="margin-left:8px;" id="interview-session-id">Loading...</span>
                        </div>
                        <div class="flex-row">
                             <span class="status-badge status-running" id="interview-status">正在连接 AI 面试官...</span>
                        </div>
                    </div>
                    <div class="card-body custom-scrollbar" id="interview-board" style="padding: 0; display:flex; flex-direction:column; justify-content:space-between; background-color: #fafbfc;">
                        <div class="interview-board" id="chat-history">
                            <!-- Init by JS -->
                        </div>
                        <div style="padding:16px;">
                            <div class="interview-input-area" id="input-container" style="opacity: 0.5; pointer-events:none;">
                                <textarea id="interview-input" class="interview-textarea" placeholder="在此输入你的回答..."></textarea>
                                <div class="interview-toolbar">
                                    <div class="toolbar-actions">
                                        <button title="开启麦克风录音"><i class="fas fa-microphone"></i></button>
                                        <button title="代码片段"><i class="fas fa-code"></i></button>
                                    </div>
                                    <button class="btn btn-primary" onclick="submitAnswer()" id="btn-submit-answer">提交回答 (Enter)</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right Analysis Area -->
            <div class="col-side custom-scrollbar">
                
                <div class="card mb-4">
                    <div class="card-header">
                        <div class="card-title"><i class="fas fa-chart-line"></i> 实时表现分析 (第 <span id="disp-round">1</span> 轮)</div>
                    </div>
                    <div class="card-body" style="padding: 16px;">
                        <div style="display:flex; justify-content:space-between; align-items:end; margin-bottom: 16px;">
                             <div>
                                 <div style="font-size:11px; color:var(--text-muted);">本轮得分</div>
                                 <div style="font-size:24px; font-weight:700; color:var(--primary);" id="cur-score">-</div>
                             </div>
                             <div>
                                 <div style="font-size:11px; color:var(--text-muted);">追问意图预判</div>
                                 <div class="tag" style="background:#f1f5f9;border:1px solid #e2e8f0;" id="cur-intent">-</div>
                             </div>
                        </div>
                        
                        <div style="font-size:12px; font-weight:600; margin-bottom:8px;">回答命中点</div>
                        <ul class="result-list text-success mb-4" id="ans-adv" style="font-size:12px;"></ul>
                        
                        <div style="font-size:12px; font-weight:600; margin-bottom:8px;">薄弱与改进点</div>
                        <ul class="result-list text-warning" id="ans-weak" style="font-size:12px;"></ul>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <div class="card-title"><i class="fas fa-bolt"></i> 面试官思考流</div>
                    </div>
                    <div class="card-body">
                        <div class="timeline" id="interviewer-timeline">
                            <div class="empty-state" style="padding: 10px;">等待回答</div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    `;
}

function initMockInterview() {
    // Mock POST /api/v1/resume/interview/start
    setTimeout(() => {
        document.getElementById('interview-session-id').textContent = 'Session: #INV-10';
        document.getElementById('interview-status').textContent = '面试中';
        document.getElementById('interview-status').className = 'status-badge status-success';
        
        currentRound = 1;
        appendInterviewerQuestion("面试官", "你好！很高兴能和你交流一下。请你先做一个简短的自我介绍，并重点说明你在 Java 后端和 AI Agent 项目中的实践。");
        
        unlockInput();
    }, 1000);
}

function unlockInput() {
    const container = document.getElementById('input-container');
    container.style.opacity = '1';
    container.style.pointerEvents = 'auto';
    document.getElementById('interview-input').focus();
    document.getElementById('disp-round').textContent = currentRound;
    
    // clear analysis
    document.getElementById('cur-score').textContent = "-";
    document.getElementById('cur-intent').textContent = "-";
    document.getElementById('ans-adv').innerHTML = "";
    document.getElementById('ans-weak').innerHTML = "";
    document.getElementById('interviewer-timeline').innerHTML = "";
}

function lockInput() {
    const container = document.getElementById('input-container');
    container.style.opacity = '0.5';
    container.style.pointerEvents = 'none';
}

function appendInterviewerQuestion(role, text) {
    const history = document.getElementById('chat-history');
    const box = document.createElement('div');
    box.className = 'chat-round';
    box.innerHTML = `
        <div class="flex-row" style="padding-left:12px; font-size:12px; font-weight:600; color:var(--text-muted);">
            <i class="fas fa-comment-dots"></i> 第 ${currentRound} 轮
        </div>
        <div class="chat-bubble ai">
            <div class="bubble-avatar"><i class="fas fa-robot"></i></div>
            <div>
                <div class="bubble-meta">高级评测 Agent (面试官)</div>
                <div class="bubble-content">${text}</div>
            </div>
        </div>
    `;
    history.appendChild(box);
    history.scrollTop = history.scrollHeight;
}

function appendUserAnswer(text) {
    const history = document.getElementById('chat-history');
    const currentRoundBox = history.lastElementChild;
    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble user';
    bubble.innerHTML = `
        <div class="bubble-avatar"><i class="fas fa-user"></i></div>
        <div>
            <div class="bubble-meta">候选人</div>
            <div class="bubble-content">${text}</div>
        </div>
    `;
    currentRoundBox.appendChild(bubble);
    history.parentElement.scrollTop = history.parentElement.scrollHeight;
}

function submitAnswer() {
    const input = document.getElementById('interview-input');
    const text = input.value.trim();
    if(!text) return;
    
    lockInput();
    appendUserAnswer(text);
    input.value = '';
    
    document.getElementById('interview-status').textContent = '面试官思考中...';
    document.getElementById('interview-status').className = 'status-badge status-running';
    
    // Mock POST /api/v1/resume/interview/answer/stream
    const timeline = document.getElementById('interviewer-timeline');
    timeline.innerHTML = '';
    
    const events = [
         { type: "analysis", subType: "intent", content: "提取候选人回答要点，匹配核心简历槽位..." },
         { type: "execution", subType: "fact_check", content: "回答逻辑清晰，提及的 AI Agent 组件确实在上下文中存在（知识库命中）。" },
         { type: "supervision", subType: "assessment", content: "自我介绍流畅度良好，技术点覆盖率 85%。但对实现原理讲得有点浅。" },
         { type: "summary", subType: "score", data: { score: "88/100", intent: "深挖底层实现与难点", adv: ["表达逻辑清晰", "技术契合度高"], weak: ["缺少对选型原理的阐释"] } },
    ];
    
    let delay = 0;
    
    events.forEach((ev, idx) => {
        delay += 600;
        setTimeout(() => {
            appendTimelineNode(timeline, ev);
            
            if(ev.subType === 'score') {
                document.getElementById('cur-score').textContent = ev.data.score;
                document.getElementById('cur-intent').textContent = ev.data.intent;
                document.getElementById('ans-adv').innerHTML = ev.data.adv.map(i => `<li><i class="fas fa-circle" style="font-size:6px;transform:translateY(-2px);"></i> ${i}</li>`).join('');
                document.getElementById('ans-weak').innerHTML = ev.data.weak.map(i => `<li><i class="fas fa-circle" style="font-size:6px;transform:translateY(-2px);"></i> ${i}</li>`).join('');
            }
        }, delay);
    });
    
    // Next question
    setTimeout(() => {
        document.getElementById('interview-status').textContent = '面试中';
        document.getElementById('interview-status').className = 'status-badge status-success';
        
        currentRound++;
        if(currentRound <= MAX_ROUNDS) {
            appendInterviewerQuestion("面试官", "听起来你的项目非常丰富。你能具体说说在 Agent 项目中，你是如何处理大模型接口超时或限流问题的吗？有没有涉及到降级策略？");
            unlockInput();
        } else {
            appendInterviewerQuestion("面试官", "好的，我们今天的面试就先到这里。后续的反馈 HR 会通过邮件通知你。非常感谢您的时间！");
            document.getElementById('interview-status').textContent = '面试结束';
            document.getElementById('disp-round').textContent = "结束";
        }
    }, delay + 800);
}

// Ensure enter key submits answer
document.addEventListener('keydown', (e) => {
    if(e.key === 'Enter' && currentView === 'mock-interview' && !e.shiftKey) {
        if(document.activeElement.id === 'interview-input') {
            e.preventDefault();
            submitAnswer();
        }
    }
});

// ----------------------
// 4. Mock Generic Pages
// ----------------------
function renderMockPage(title, icon, message) {
    return `
        <div class="card h-full" style="height: calc(100vh - 120px); display: flex; align-items:center; justify-content:center;">
             <div class="empty-state">
                 <i class="${icon} empty-icon" style="color:var(--primary); font-size: 64px;"></i>
                 <div class="empty-title" style="font-size:24px; margin-top:20px;">${title}</div>
                 <div class="empty-desc" style="font-size:14px; max-width: 400px; line-height:1.6; margin-top:10px;">${message}</div>
             </div>
        </div>
    `;
}
