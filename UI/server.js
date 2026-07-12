const express = require('express');
const path = require('path');

const app = express();
const PORT = 3000;
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8084';

app.use(express.static(__dirname));
app.use(express.json());

app.post('/api/convert', async (req, res) => {
    const { amount, from, to } = req.query;
    try {
        const response = await fetch(
            `${API_BASE_URL}/api/currencies/convert?amount=${amount}&from=${from}&to=${to}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-API-KEY': 'SUPER-SECRET-DEV-KEY-123'
                }
            }
        );

        if (!response.ok) {
            return res.status(response.status).json({ error: 'Failed to communicate with Spring Boot' });
        }

        const data = await response.json();
        res.json(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/warning-check', async (req, res) => {
    const { amount, currency } = req.query;
    try {
        const response = await fetch(
            `${API_BASE_URL}/api/currencies/warning-check?amount=${amount}&currency=${currency}`
        );

        if (!response.ok) {
            return res.status(response.status).json({ error: 'Failed to communicate with Spring Boot' });
        }

        const data = await response.text();
        res.send(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.listen(PORT, () => {
    console.log(`Server running and accessible at: http://localhost:${PORT}`);
});