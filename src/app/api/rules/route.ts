import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

// GET - Fetch all rules
export async function GET() {
  try {
    const rules = await db.autoReplyRule.findMany({
      orderBy: { priority: 'desc' }
    })
    return NextResponse.json({ success: true, rules })
  } catch (error) {
    console.error('Error fetching rules:', error)
    return NextResponse.json(
      { error: 'Failed to fetch rules' },
      { status: 500 }
    )
  }
}

// POST - Create a new rule
export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { trigger, response, useAI, enabled, priority } = body

    if (!trigger || typeof trigger !== 'string') {
      return NextResponse.json(
        { error: 'Trigger is required' },
        { status: 400 }
      )
    }

    // Check if trigger already exists
    const existing = await db.autoReplyRule.findFirst({
      where: { trigger: trigger.toLowerCase().trim() }
    })

    if (existing) {
      return NextResponse.json(
        { error: 'A rule with this trigger already exists' },
        { status: 400 }
      )
    }

    const rule = await db.autoReplyRule.create({
      data: {
        trigger: trigger.toLowerCase().trim(),
        response: response || null,
        useAI: useAI ?? false,
        enabled: enabled ?? true,
        priority: priority ?? 0
      }
    })

    return NextResponse.json({ success: true, rule })
  } catch (error) {
    console.error('Error creating rule:', error)
    return NextResponse.json(
      { error: 'Failed to create rule' },
      { status: 500 }
    )
  }
}

// PUT - Update a rule
export async function PUT(request: NextRequest) {
  try {
    const body = await request.json()
    const { id, trigger, response, useAI, enabled, priority } = body

    if (!id) {
      return NextResponse.json(
        { error: 'Rule ID is required' },
        { status: 400 }
      )
    }

    const rule = await db.autoReplyRule.update({
      where: { id },
      data: {
        trigger: trigger?.toLowerCase().trim(),
        response,
        useAI,
        enabled,
        priority
      }
    })

    return NextResponse.json({ success: true, rule })
  } catch (error) {
    console.error('Error updating rule:', error)
    return NextResponse.json(
      { error: 'Failed to update rule' },
      { status: 500 }
    )
  }
}

// DELETE - Delete a rule
export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const id = searchParams.get('id')

    if (!id) {
      return NextResponse.json(
        { error: 'Rule ID is required' },
        { status: 400 }
      )
    }

    await db.autoReplyRule.delete({
      where: { id }
    })

    return NextResponse.json({ success: true })
  } catch (error) {
    console.error('Error deleting rule:', error)
    return NextResponse.json(
      { error: 'Failed to delete rule' },
      { status: 500 }
    )
  }
}
