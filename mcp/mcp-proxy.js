#!/usr/bin/env node

import fetch from 'node-fetch';
import { createInterface } from 'readline';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Create a log file for debugging
const logFile = path.join(__dirname, 'mcp-proxy.log');
const logStream = fs.createWriteStream(logFile, { flags: 'a' });

function log(...args) {
  const timestamp = new Date().toISOString();
  const message = `[${timestamp}] ${args.join(' ')}`;
  console.error(message);
  logStream.write(message + '\n');
}

// Log startup
log('=== MCP Proxy Starting ===');
log('Node version:', process.version);
log('Working directory:', process.cwd());
log('Script location:', __filename);

const baseUrl = 'http://localhost:8080/mcp';
log('Base URL:', baseUrl);

// Define tools
const tools = [
  {
    name: 'get_task_schema',
    description: 'Get the database schema for tasks table',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'insert_tasks',
    description: 'Insert tasks into the database',
    inputSchema: {
      type: 'object',
      properties: {
        tasks: {
          type: 'array',
          description: 'Array of task objects',
          items: {
            type: 'object',
            properties: {
              title: { type: 'string' },
              description: { type: 'string' },
              status: { type: 'string', enum: ['TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED', 'ON_HOLD'] },
              priority: { type: 'string', enum: ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] },
              dueDate: { type: 'string' },
              assignedTo: { type: 'string' },
              tags: { type: 'string' }
            },
            required: ['title', 'status', 'priority']
          }
        }
      },
      required: ['tasks']
    }
  },
  {
    name: 'get_tasks_summary',
    description: 'Get comprehensive task statistics',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'get_all_tasks',
    description: 'Get all tasks with pagination',
    inputSchema: {
      type: 'object',
      properties: {
        page: { type: 'number', default: 0 },
        size: { type: 'number', default: 100 }
      }
    }
  },
  {
    name: 'get_help',
    description: 'Get help information about the MCP server',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  }
];

log('Tools defined:', tools.length);

// Test server connectivity on startup
async function testServer() {
  try {
    log('Testing server connectivity...');
    const response = await fetch(baseUrl + '/help', { timeout: 5000 });
    if (response.ok) {
      log('✓ Server is reachable');
      return true;
    } else {
      log('✗ Server returned status:', response.status);
      return false;
    }
  } catch (err) {
    log('✗ Server connectivity test failed:', err.message);
    return false;
  }
}

// Test server before starting
testServer();

const rl = createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

log('Readline interface created');

rl.on('line', async (line) => {
  log('Received line:', line.substring(0, 200));

  let msg;
  try {
    msg = JSON.parse(line);
    log('Parsed message, method:', msg.method, 'id:', msg.id);
  } catch (err) {
    log('ERROR: Failed to parse JSON:', err.message);
    return;
  }

  const { id, method, params } = msg;

  try {
    // Handle initialize
    if (method === 'initialize') {
      log('Handling initialize request');
      const response = {
        jsonrpc: '2.0',
        id,
        result: {
          protocolVersion: '2024-11-05',
          capabilities: {
            tools: {}
          },
          serverInfo: {
            name: 'task-manager-mcp',
            version: '1.0.0'
          }
        }
      };
      const responseStr = JSON.stringify(response);
      process.stdout.write(responseStr + '\n');
      log('Sent initialize response');
      return;
    }

    // Handle tools/list
    if (method === 'tools/list') {
      log('Handling tools/list request');
      const response = {
        jsonrpc: '2.0',
        id,
        result: {
          tools: tools
        }
      };
      const responseStr = JSON.stringify(response);
      process.stdout.write(responseStr + '\n');
      log('Sent tools list with', tools.length, 'tools');
      return;
    }

    // Handle tools/call
    if (method === 'tools/call') {
      const toolName = params?.name;
      const toolArgs = params?.arguments || {};

      log('Handling tools/call:', toolName);
      log('Arguments:', JSON.stringify(toolArgs).substring(0, 200));

      let endpoint;
      let httpMethod = 'GET';
      let body = null;

      switch (toolName) {
        case 'get_task_schema':
          endpoint = '/schema/tasks';
          break;
        case 'insert_tasks':
          endpoint = '/tasks';
          httpMethod = 'POST';
          body = JSON.stringify(toolArgs.tasks || []);
          log('Request body length:', body.length);
          break;
        case 'get_tasks_summary':
          endpoint = '/tasks/summary';
          break;
        case 'get_all_tasks':
          const page = toolArgs.page || 0;
          const size = toolArgs.size || 100;
          endpoint = `/tasks?page=${page}&size=${size}`;
          break;
        case 'get_help':
          endpoint = '/help';
          break;
        default:
          throw new Error(`Unknown tool: ${toolName}`);
      }

      const url = baseUrl + endpoint;
      log('Calling:', httpMethod, url);

      const fetchOptions = {
        method: httpMethod,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        timeout: 30000
      };

      if (body) {
        fetchOptions.body = body;
      }

      const response = await fetch(url, fetchOptions);
      log('Response status:', response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        log('Error response:', errorText);
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const data = await response.json();
      log('Response received, data keys:', Object.keys(data).join(', '));

      const result = {
        jsonrpc: '2.0',
        id,
        result: {
          content: [
            {
              type: 'text',
              text: JSON.stringify(data, null, 2)
            }
          ]
        }
      };

      const resultStr = JSON.stringify(result);
      process.stdout.write(resultStr + '\n');
      log('Response sent to Claude, length:', resultStr.length);
      return;
    }

    // Handle notifications
    if (method === 'notifications/initialized') {
      log('Received initialized notification');
      return;
    }

    // Unknown method
    log('WARNING: Unknown method:', method);
    const errorResponse = {
      jsonrpc: '2.0',
      id,
      error: {
        code: -32601,
        message: `Method not found: ${method}`
      }
    };
    process.stdout.write(JSON.stringify(errorResponse) + '\n');

  } catch (err) {
    log('ERROR in message handler:', err.message);
    log('Stack:', err.stack);

    const errorResponse = {
      jsonrpc: '2.0',
      id,
      error: {
        code: -32000,
        message: err.message,
        data: { stack: err.stack }
      }
    };
    process.stdout.write(JSON.stringify(errorResponse) + '\n');
  }
});

rl.on('close', () => {
  log('Readline interface closed');
  logStream.end();
  process.exit(0);
});

// Error handlers
process.on('uncaughtException', (err) => {
  log('UNCAUGHT EXCEPTION:', err.message);
  log('Stack:', err.stack);
  logStream.end();
  process.exit(1);
});

process.on('unhandledRejection', (err) => {
  log('UNHANDLED REJECTION:', err);
  logStream.end();
  process.exit(1);
});

process.on('SIGINT', () => {
  log('Received SIGINT, shutting down gracefully');
  logStream.end();
  process.exit(0);
});

process.on('SIGTERM', () => {
  log('Received SIGTERM, shutting down gracefully');
  logStream.end();
  process.exit(0);
});

log('=== MCP Proxy Ready ===');
log('Waiting for messages from Claude Desktop...');
log('Log file:', logFile);