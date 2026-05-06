import { NextRequest, NextResponse } from 'next/server'
import { db } from '@/lib/db'

// GET - Fetch app settings
export async function GET() {
  try {
    let settings = await db.appSettings.findFirst()

    if (!settings) {
      settings = await db.appSettings.create({
        data: {}
      })
    }

    return NextResponse.json({ success: true, settings })
  } catch (error) {
    console.error('Error fetching settings:', error)
    return NextResponse.json(
      { error: 'Failed to fetch settings' },
      { status: 500 }
    )
  }
}

// POST - Update app settings
export async function POST(request: NextRequest) {
  try {
    const body = await request.json()

    let settings = await db.appSettings.findFirst()

    if (!settings) {
      settings = await db.appSettings.create({
        data: body
      })
    } else {
      settings = await db.appSettings.update({
        where: { id: settings.id },
        data: body
      })
    }

    return NextResponse.json({ success: true, settings })
  } catch (error) {
    console.error('Error updating settings:', error)
    return NextResponse.json(
      { error: 'Failed to update settings' },
      { status: 500 }
    )
  }
}

// PUT - Update app settings (same as POST)
export async function PUT(request: NextRequest) {
  try {
    const body = await request.json()

    let settings = await db.appSettings.findFirst()

    if (!settings) {
      settings = await db.appSettings.create({
        data: body
      })
    } else {
      settings = await db.appSettings.update({
        where: { id: settings.id },
        data: body
      })
    }

    return NextResponse.json({ success: true, settings })
  } catch (error) {
    console.error('Error updating settings:', error)
    return NextResponse.json(
      { error: 'Failed to update settings' },
      { status: 500 }
    )
  }
}
