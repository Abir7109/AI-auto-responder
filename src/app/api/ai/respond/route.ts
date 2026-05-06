import { NextRequest, NextResponse } from 'next/server'
import ZAI from 'z-ai-web-dev-sdk'

// Persona prompts for different response styles
const personaPrompts: Record<string, string> = {
  professional: 'You are a professional assistant. Respond formally and concisely. Keep responses brief and to the point.',
  friendly: 'You are a friendly assistant. Respond warmly and casually, like chatting with a friend. Use a conversational tone.',
  witty: 'You are a witty assistant. Respond with humor and charm while being helpful. Keep it light and engaging.',
  minimal: 'You are a minimal assistant. Respond with only the essential information. Be extremely brief and direct.',
}

interface ConversationState {
  messages: Array<{ role: 'assistant' | 'user'; content: string }>
  lastAccessed: number
}

// In-memory conversation store (in production, use a database)
const conversations = new Map<string, ConversationState>()

// Clean up old conversations (older than 1 hour)
setInterval(() => {
  const now = Date.now()
  for (const [id, state] of conversations.entries()) {
    if (now - state.lastAccessed > 3600000) {
      conversations.delete(id)
    }
  }
}, 300000) // Run every 5 minutes

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { message, persona = 'professional', sessionId, conversationHistory } = body

    if (!message || typeof message !== 'string') {
      return NextResponse.json(
        { error: 'Message is required' },
        { status: 400 }
      )
    }

    // Initialize ZAI
    const zai = await ZAI.create()

    // Get or create conversation state
    const convId = sessionId || 'default'
    let conversation = conversations.get(convId)

    if (!conversation) {
      conversation = {
        messages: [
          {
            role: 'assistant',
            content: personaPrompts[persona] || personaPrompts.professional
          }
        ],
        lastAccessed: Date.now()
      }
      conversations.set(convId, conversation)
    }

    // Add user message to conversation
    conversation.messages.push({
      role: 'user',
      content: message
    })

    // Keep only last 10 messages to avoid token limits
    if (conversation.messages.length > 10) {
      conversation.messages = [
        conversation.messages[0], // Keep system prompt
        ...conversation.messages.slice(-9) // Keep last 9 messages
      ]
    }

    // Get completion from AI
    const completion = await zai.chat.completions.create({
      messages: conversation.messages,
      thinking: { type: 'disabled' }
    })

    const aiResponse = completion.choices[0]?.message?.content

    if (!aiResponse) {
      return NextResponse.json(
        { error: 'Failed to generate response' },
        { status: 500 }
      )
    }

    // Add AI response to conversation history
    conversation.messages.push({
      role: 'assistant',
      content: aiResponse
    })
    conversation.lastAccessed = Date.now()

    return NextResponse.json({
      success: true,
      response: aiResponse,
      sessionId: convId
    })

  } catch (error) {
    console.error('AI Response Error:', error)
    return NextResponse.json(
      { error: 'Failed to process AI request', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    )
  }
}

// GET endpoint to test API status
export async function GET() {
  return NextResponse.json({
    status: 'ok',
    message: 'AI Auto-Responder API is running',
    version: '1.0.0',
    features: ['chat', 'multi-persona', 'conversation-history']
  })
}
