// const sendBtn = document.getElementById('send-btn');
// const userInput = document.getElementById('user-input');
// const chatBox = document.getElementById('chat-box');
// const userId = "franchise-user-1"; // Change as needed
//
// function appendMessage(text, isUser = false) {
//     const msg = document.createElement('div');
//     msg.classList.add('message', isUser ? 'user-msg' : 'bot-msg');
//     msg.textContent = text;
//     chatBox.appendChild(msg);
//     chatBox.scrollTop = chatBox.scrollHeight;
// }
//
// sendBtn.addEventListener('click', async () => {
//     const question = userInput.value.trim();
//     if (!question) return;
//
//     appendMessage(question, true);
//     userInput.value = '';
//
//     try {
//         const response = await fetch(`/${userId}/chat?question=${encodeURIComponent(question)}`);
//         const result = await response.text();
//         appendMessage(result);
//     } catch (error) {
//         console.error('Chat error:', error);
//         appendMessage("Something went wrong. Please try again.", false);
//     }
// });

async function sendMessage() {
    const userInput = document.getElementById("user-input");
    const chatBox = document.getElementById("chat-box");
    const question = userInput.value.trim();

    if (!question) return;

    // Add user message
    const userRow = document.createElement("div");
    userRow.className = "message-row user";
    userRow.innerHTML = `<div class="message user-msg">${question}</div>`;
    chatBox.appendChild(userRow);

    userInput.value = "";
    chatBox.scrollTop = chatBox.scrollHeight;

    try {
        const userId = "himanshu"; // replace as needed
        const response = await fetch(`/${userId}/chat?question=${encodeURIComponent(question)}`);
        const answer = await response.text();

        // Convert newlines to <br>
        const formattedAnswer = answer.replace(/\n/g, "<br>");

        const botRow = document.createElement("div");
        botRow.className = "message-row bot";
        botRow.innerHTML = `<div class="message bot-msg">${formattedAnswer}</div>`;
        chatBox.appendChild(botRow);

        chatBox.scrollTop = chatBox.scrollHeight;
    } catch (error) {
        const errorRow = document.createElement("div");
        errorRow.className = "message-row bot";
        errorRow.innerHTML = `<div class="message bot-msg">Error: Unable to get response.</div>`;
        chatBox.appendChild(errorRow);
    }
}
