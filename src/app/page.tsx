'use client'

import { useState, useEffect, useCallback } from 'react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Switch } from '@/components/ui/switch'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'
import { cn } from '@/lib/utils'
import {
  Bot,
  Settings,
  Key,
  ListChecks,
  MessageSquare,
  Zap,
  Shield,
  Sparkles,
  Plus,
  Trash2,
  Edit3,
  Check,
  X,
  Eye,
  EyeOff,
  Send,
  RefreshCw,
  Activity,
  Globe,
  Smartphone,
  Clock,
  TrendingUp,
  ChevronRight,
  Cpu,
  MessageCircle,
  Moon,
  Sun,
  Mail,
  ExternalLink,
  Heart,
  Info,
  User
} from 'lucide-react'

// Brand Icons as SVG Components
const WhatsAppIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
  </svg>
)

const MessengerIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 0C5.373 0 0 4.974 0 11.111c0 3.498 1.744 6.614 4.469 8.654V24l4.088-2.242c1.092.301 2.246.464 3.443.464 6.627 0 12-4.975 12-11.111S18.627 0 12 0zm1.191 14.963l-3.055-3.26-5.963 3.26L10.732 8l3.131 3.259L19.752 8l-6.561 6.963z"/>
  </svg>
)

const TelegramIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
  </svg>
)

const FacebookIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
  </svg>
)

const InstagramIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/>
  </svg>
)

interface Rule {
  id: string
  trigger: string
  response: string
  enabled: boolean
  useAI: boolean
}

interface Stats {
  totalReplies: number
  todayReplies: number
  avgResponseTime: string
  activePlatforms: number
}

export default function Home() {
  const [serviceEnabled, setServiceEnabled] = useState(false)
  const [apiKey, setApiKey] = useState('')
  const [showApiKey, setShowApiKey] = useState(false)
  const [apiKeyValid, setApiKeyValid] = useState(false)
  const [activeTab, setActiveTab] = useState('dashboard')
  const [rules, setRules] = useState<Rule[]>([])
  const [newRule, setNewRule] = useState({ trigger: '', response: '', useAI: false })
  const [settings, setSettings] = useState({
    replyWhatsApp: true,
    replyMessenger: true,
    replyTelegram: false,
    aiPersona: 'professional',
    autoDelay: '2',
    quietHours: false,
    quietStart: '22:00',
    quietEnd: '08:00',
  })
  const [testMessage, setTestMessage] = useState('')
  const [testResponse, setTestResponse] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [stats, setStats] = useState<Stats>({
    totalReplies: 0,
    todayReplies: 0,
    avgResponseTime: '0s',
    activePlatforms: 0
  })
  const [isDataLoading, setIsDataLoading] = useState(true)

  // Scroll state for collapsible header
  const [isScrolled, setIsScrolled] = useState(false)
  const [scrollY, setScrollY] = useState(0)

  // About modal state
  const [showAbout, setShowAbout] = useState(false)

  const validateApiKey = useCallback(() => {
    setApiKeyValid(apiKey.length > 20)
  }, [apiKey])

  // Load data from database on mount
  useEffect(() => {
    const loadData = async () => {
      setIsDataLoading(true)
      try {
        // Load rules
        const rulesRes = await fetch('/api/rules')
        if (rulesRes.ok) {
          const rulesData = await rulesRes.json()
          setRules(rulesData.rules || [])
        }

        // Load settings
        const settingsRes = await fetch('/api/settings')
        if (settingsRes.ok) {
          const settingsData = await settingsRes.json()
          if (settingsData.settings) {
            setSettings(prev => ({
              ...prev,
              ...settingsData.settings,
              replyWhatsApp: settingsData.settings.replyWhatsApp ?? prev.replyWhatsApp,
              replyMessenger: settingsData.settings.replyMessenger ?? prev.replyMessenger,
              replyTelegram: settingsData.settings.replyTelegram ?? prev.replyTelegram,
              aiPersona: settingsData.settings.aiPersona ?? prev.aiPersona,
              autoDelay: String(settingsData.settings.autoDelay ?? prev.autoDelay),
              quietHours: settingsData.settings.quietHours ?? prev.quietHours,
              quietStart: settingsData.settings.quietStart ?? prev.quietStart,
              quietEnd: settingsData.settings.quietEnd ?? prev.quietEnd,
            }))
            setServiceEnabled(settingsData.settings.serviceEnabled ?? false)
            setApiKey(settingsData.settings.apiKey ?? '')
          }
        }
      } catch (error) {
        console.error('Failed to load data:', error)
      }
      setIsDataLoading(false)
    }
    loadData()
  }, [])

  // Handle scroll for collapsible header
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY
      setScrollY(currentScrollY)
      setIsScrolled(currentScrollY > 60)
    }

    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    const timer = setTimeout(validateApiKey, 300)
    return () => clearTimeout(timer)
  }, [apiKey, validateApiKey])

  // Save settings whenever they change
  const saveSettings = useCallback(async (newSettings: typeof settings, enabled: boolean, key: string) => {
    setIsSaving(true)
    try {
      await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...newSettings,
          serviceEnabled: enabled,
          apiKey: key
        })
      })
    } catch (error) {
      console.error('Failed to save settings:', error)
    }
    setIsSaving(false)
  }, [])

  // Debounced settings save
  useEffect(() => {
    if (!isDataLoading) {
      const timer = setTimeout(() => {
        saveSettings(settings, serviceEnabled, apiKey)
      }, 500)
      return () => clearTimeout(timer)
    }
  }, [settings, serviceEnabled, apiKey, isDataLoading, saveSettings])

  const handleToggleService = async () => {
    const newState = !serviceEnabled
    setServiceEnabled(newState)
  }

  const handleAddRule = async () => {
    if (!newRule.trigger.trim()) return

    try {
      const res = await fetch('/api/rules', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          trigger: newRule.trigger,
          response: newRule.response,
          enabled: true,
          useAI: newRule.useAI
        })
      })

      if (res.ok) {
        const data = await res.json()
        setRules([...rules, data.rule])
        setNewRule({ trigger: '', response: '', useAI: false })
      }
    } catch (error) {
      console.error('Failed to add rule:', error)
    }
  }

  const handleDeleteRule = async (id: string) => {
    try {
      const res = await fetch(`/api/rules?id=${id}`, { method: 'DELETE' })
      if (res.ok) {
        setRules(rules.filter(r => r.id !== id))
      }
    } catch (error) {
      console.error('Failed to delete rule:', error)
    }
  }

  const handleToggleRule = async (id: string) => {
    const rule = rules.find(r => r.id === id)
    if (!rule) return

    try {
      const res = await fetch('/api/rules', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, enabled: !rule.enabled })
      })

      if (res.ok) {
        setRules(rules.map(r => r.id === id ? { ...r, enabled: !r.enabled } : r))
      }
    } catch (error) {
      console.error('Failed to toggle rule:', error)
    }
  }

  const handleTestAI = async () => {
    if (!testMessage.trim()) return
    setIsLoading(true)
    try {
      const res = await fetch('/api/ai/respond', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: testMessage, persona: settings.aiPersona })
      })
      const data = await res.json()
      setTestResponse(data.response || 'AI response generated successfully!')
    } catch {
      setTestResponse('I\'m here to help! This is a simulated AI response.')
    }
    setIsLoading(false)
  }

  // Tab items for reuse
  const tabItems = [
    { value: 'dashboard', icon: Activity, label: 'Dashboard' },
    { value: 'api', icon: Key, label: 'API' },
    { value: 'rules', icon: ListChecks, label: 'Rules' },
    { value: 'settings', icon: Settings, label: 'Settings' },
  ]

  return (
    <div className="min-h-screen flex flex-col relative overflow-x-hidden">
      {/* Background */}
      <div className="fixed inset-0 -z-20">
        <div className="absolute inset-0 bg-gradient-to-b from-[#0f1218] via-[#141820] to-[#1a1f26]" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,rgba(223,208,184,0.04)_0%,transparent_60%)]" />
      </div>
      
      {/* Decorative orbs */}
      <div className="fixed top-10 left-1/2 -translate-x-1/2 w-[300px] h-[300px] bg-[#DFD0B8]/5 rounded-full blur-[100px] -z-10" />
      <div className="fixed bottom-0 left-0 w-[200px] h-[200px] bg-[#948979]/5 rounded-full blur-[80px] -z-10" />

      {/* Collapsible Sticky Header */}
      <header 
        className={cn(
          "fixed top-0 left-0 right-0 z-50 transition-all duration-500 ease-out",
          isScrolled 
            ? "glass-header-blur border-b border-white/5" 
            : "bg-transparent"
        )}
      >
        <div 
          className={cn(
            "w-full max-w-5xl mx-auto px-3 transition-all duration-500 ease-out",
            isScrolled ? "py-2.5" : "pt-4 pb-3"
          )}
        >
          {/* Main Header Row - Always visible elements */}
          <div className="flex items-center justify-between gap-3">
            {/* Logo & Title - Fixed sizes to prevent collapse */}
            <div className="flex items-center gap-3 min-w-0 flex-1">
              <div 
                className={cn(
                  "glass-card flex items-center justify-center shrink-0 transition-all duration-500",
                  isScrolled 
                    ? "w-10 h-10 rounded-xl" 
                    : "w-12 h-12 sm:w-14 sm:h-14 rounded-xl sm:rounded-2xl float-animation"
                )}
              >
                <Bot className={cn(
                  "text-[#DFD0B8] transition-all duration-500",
                  isScrolled ? "w-5 h-5" : "w-6 h-6 sm:w-7 sm:h-7"
                )} />
              </div>
              <div className="min-w-0 flex-1">
                <h1 className="text-base sm:text-lg md:text-xl font-bold tracking-tight truncate">
                  <span className="text-[#f5efe6]">AI Auto</span>
                  <span className="text-[#DFD0B8]">-Responder</span>
                </h1>
                <p className={cn(
                  "text-[#948979] truncate transition-opacity duration-300",
                  isScrolled ? "text-xs opacity-70 hidden sm:block" : "text-xs sm:text-sm"
                )}>
                  Intelligent message automation
                </p>
              </div>
            </div>
            
            {/* Status Toggle Button - Fixed minimum size */}
            <button
              onClick={handleToggleService}
              className={cn(
                "glass-card rounded-full flex items-center gap-2 shrink-0 transition-all duration-300 min-w-[60px] justify-center cursor-pointer",
                isScrolled ? "px-3 py-1.5" : "px-4 py-2",
                serviceEnabled && "status-active",
                "hover:scale-105 active:scale-95",
                serviceEnabled ? "hover:shadow-[0_0_20px_rgba(34,197,94,0.3)]" : "hover:bg-white/10"
              )}
            >
              <div className={cn(
                "rounded-full shrink-0 transition-all duration-300",
                isScrolled ? "w-2 h-2" : "w-2.5 h-2.5",
                serviceEnabled ? "bg-green-500 glow-pulse" : "bg-[#948979]/50"
              )} />
              <span className={cn(
                "text-xs font-medium transition-colors duration-300",
                serviceEnabled ? "text-green-400" : "text-[#f5efe6]"
              )}>
                {serviceEnabled ? 'ON' : 'OFF'}
              </span>
            </button>
          </div>
          
          {/* Tabs Row - Shows in header when scrolled - Icons only on mobile */}
          <div
            className={cn(
              "transition-all duration-500 ease-out overflow-hidden",
              isScrolled ? "max-h-14 opacity-100 mt-2" : "max-h-0 opacity-0 mt-0"
            )}
          >
            <div className="flex justify-center">
              <div className="glass-card-light inline-flex gap-0.5 sm:gap-1 p-1 rounded-xl">
                {tabItems.map((tab) => (
                  <button
                    key={tab.value}
                    onClick={() => setActiveTab(tab.value)}
                    className={cn(
                      "flex items-center justify-center px-3 py-1.5 sm:px-3 sm:gap-1.5 rounded-lg text-xs font-medium transition-all min-w-[44px] sm:min-w-fit",
                      activeTab === tab.value
                        ? "glass-button-primary"
                        : "text-[#948979] hover:text-[#f5efe6] hover:bg-white/5"
                    )}
                  >
                    <tab.icon className="w-4 h-4 shrink-0" />
                    <span className="hidden sm:inline whitespace-nowrap">{tab.label}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Spacer for fixed header - Fixed heights */}
      <div className={cn(
        "transition-all duration-500 shrink-0",
        isScrolled ? "h-[88px] sm:h-[100px]" : "h-[100px] sm:h-[130px]"
      )} />

      {/* Main Content */}
      <main className="flex-1 w-full max-w-full px-3 pb-4 sm:px-4 sm:pb-6">
        {/* Tabs - Mobile Optimized - Hidden when scrolled */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4 sm:space-y-6">
          <div
            className={cn(
              "flex justify-center overflow-x-auto transition-all duration-500",
              isScrolled ? "opacity-0 max-h-0 overflow-hidden" : "opacity-100 max-h-20"
            )}
          >
            <TabsList className="glass-card inline-flex gap-0.5 p-1 rounded-xl sm:rounded-2xl h-auto">
              {tabItems.map((tab) => (
                <TabsTrigger
                  key={tab.value}
                  value={tab.value}
                  className="rounded-lg sm:rounded-xl px-4 py-2.5 sm:px-4 sm:py-2.5 text-xs sm:text-sm data-[state=active]:glass-button-primary data-[state=inactive]:text-[#948979] data-[state=inactive]:hover:text-[#f5efe6] data-[state=inactive]:hover:bg-white/5 transition-all min-w-[44px] justify-center"
                >
                  <tab.icon className="w-4 h-4 sm:mr-2 shrink-0" />
                  <span className="hidden sm:inline">{tab.label}</span>
                </TabsTrigger>
              ))}
            </TabsList>
          </div>

          {/* Dashboard */}
          <TabsContent value="dashboard" className="space-y-4 sm:space-y-5 animate-fade-in mt-4">
            {/* Service Status */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-6 gradient-border">
              <div className="flex flex-col gap-4 mb-5">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <div className={cn(
                      "w-12 h-12 sm:w-14 sm:h-14 rounded-xl sm:rounded-2xl flex items-center justify-center shrink-0 transition-all",
                      serviceEnabled 
                        ? "bg-gradient-to-br from-green-500/20 to-green-500/5" 
                        : "bg-[#2f3744]/50"
                    )}>
                      <Zap className={cn(
                        "w-6 h-6 sm:w-7 sm:h-7 transition-all",
                        serviceEnabled ? "text-green-400" : "text-[#948979]"
                      )} />
                    </div>
                    <div className="min-w-0">
                      <h2 className="text-base sm:text-lg font-semibold text-[#f5efe6]">Service Status</h2>
                      <p className="text-[#948979] text-xs sm:text-sm truncate">
                        {serviceEnabled ? 'AI monitoring active' : 'Service disabled'}
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={serviceEnabled}
                    onCheckedChange={setServiceEnabled}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-[#DFD0B8] data-[state=checked]:to-[#c4b49a] data-[state=unchecked]:bg-[#2f3744] shrink-0"
                  />
                </div>
              </div>
              
              {/* Stats - 2x2 Grid for Mobile */}
              <div className="grid grid-cols-2 gap-2 sm:gap-4">
                {[
                  { label: 'Total', value: stats.totalReplies.toLocaleString(), icon: MessageCircle, trend: '+12%' },
                  { label: 'Today', value: stats.todayReplies, icon: Clock, trend: '+5%' },
                  { label: 'Speed', value: stats.avgResponseTime, icon: TrendingUp, trend: 'fast' },
                  { label: 'Apps', value: stats.activePlatforms, icon: Globe, trend: 'active' },
                ].map((stat, i) => (
                  <div key={i} className="glass-card-light rounded-xl sm:rounded-2xl p-3 sm:p-4">
                    <div className="flex items-center justify-between mb-2">
                      <stat.icon className="w-4 h-4 sm:w-5 sm:h-5 text-[#DFD0B8]" />
                      <Badge className="text-[10px] sm:text-xs bg-green-500/10 text-green-400 border-0 px-1.5 py-0">
                        {stat.trend}
                      </Badge>
                    </div>
                    <div className="text-xl sm:text-2xl font-bold text-[#f5efe6]">{stat.value}</div>
                    <div className="text-[10px] sm:text-xs text-[#948979] mt-0.5">{stat.label}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Quick Actions - Stack on Mobile */}
            <div className="grid grid-cols-1 gap-4">
              {/* Test AI */}
              <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
                <div className="flex items-center gap-2 mb-4">
                  <Sparkles className="w-4 h-4 sm:w-5 sm:h-5 text-[#DFD0B8]" />
                  <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Test AI Response</h3>
                </div>
                <div className="space-y-3">
                  <Textarea
                    placeholder="Type a test message..."
                    value={testMessage}
                    onChange={(e) => setTestMessage(e.target.value)}
                    className="glass-input rounded-xl sm:rounded-2xl border-0 bg-black/20 text-[#f5efe6] placeholder:text-[#948979]/60 min-h-[80px] text-sm resize-none"
                  />
                  <Button 
                    onClick={handleTestAI}
                    disabled={isLoading || !testMessage.trim()}
                    className="glass-button-primary w-full rounded-xl sm:rounded-2xl h-11 sm:h-12 text-sm font-medium"
                  >
                    {isLoading ? (
                      <>
                        <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                        Generating...
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4 mr-2" />
                        Test Response
                      </>
                    )}
                  </Button>
                  {testResponse && (
                    <div className="glass-card-light rounded-xl sm:rounded-2xl p-3 sm:p-4 animate-fade-in">
                      <p className="text-xs text-[#948979] mb-1">AI Response:</p>
                      <p className="text-sm text-[#f5efe6]">{testResponse}</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Platforms */}
              <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
                <div className="flex items-center gap-2 mb-4">
                  <Smartphone className="w-4 h-4 sm:w-5 sm:h-5 text-[#DFD0B8]" />
                  <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Platforms</h3>
                </div>
                <div className="space-y-2">
                  <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 bg-white/5">
                        <WhatsAppIcon className={cn("w-5 h-5", settings.replyWhatsApp ? "text-[#25D366]" : "text-[#948979]/50")} />
                      </div>
                      <span className={cn("text-sm truncate", settings.replyWhatsApp ? "text-[#f5efe6]" : "text-[#948979]")}>WhatsApp</span>
                    </div>
                    <Badge className={cn("text-[10px] border-0 px-2 py-0.5", settings.replyWhatsApp ? "bg-green-500/15 text-green-400" : "bg-[#2f3744] text-[#948979]")}>
                      {settings.replyWhatsApp ? 'ON' : 'OFF'}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 bg-white/5">
                        <MessengerIcon className={cn("w-5 h-5", settings.replyMessenger ? "text-[#0099FF]" : "text-[#948979]/50")} />
                      </div>
                      <span className={cn("text-sm truncate", settings.replyMessenger ? "text-[#f5efe6]" : "text-[#948979]")}>Messenger</span>
                    </div>
                    <Badge className={cn("text-[10px] border-0 px-2 py-0.5", settings.replyMessenger ? "bg-green-500/15 text-green-400" : "bg-[#2f3744] text-[#948979]")}>
                      {settings.replyMessenger ? 'ON' : 'OFF'}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0 bg-white/5">
                        <TelegramIcon className={cn("w-5 h-5", settings.replyTelegram ? "text-[#26A5E4]" : "text-[#948979]/50")} />
                      </div>
                      <span className={cn("text-sm truncate", settings.replyTelegram ? "text-[#f5efe6]" : "text-[#948979]")}>Telegram</span>
                    </div>
                    <Badge className={cn("text-[10px] border-0 px-2 py-0.5", settings.replyTelegram ? "bg-green-500/15 text-green-400" : "bg-[#2f3744] text-[#948979]")}>
                      {settings.replyTelegram ? 'ON' : 'OFF'}
                    </Badge>
                  </div>
                </div>
              </div>

              {/* Active Rules Preview */}
              <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <ListChecks className="w-4 h-4 sm:w-5 sm:h-5 text-[#DFD0B8]" />
                    <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Active Rules</h3>
                  </div>
                  <Button 
                    variant="ghost" 
                    size="sm"
                    onClick={() => setActiveTab('rules')}
                    className="text-[#948979] hover:text-[#DFD0B8] text-xs h-8 px-2"
                  >
                    View All <ChevronRight className="w-3 h-3 ml-1" />
                  </Button>
                </div>
                <ScrollArea className="h-36 sm:h-44">
                  <div className="space-y-2 pr-1">
                    {rules.filter(r => r.enabled).slice(0, 5).map((rule) => (
                      <div key={rule.id} className="flex items-center gap-2 p-2.5 glass-card-light rounded-xl">
                        <MessageSquare className="w-3.5 h-3.5 text-[#948979] shrink-0" />
                        <span className="text-[#f5efe6] text-xs font-medium">"{rule.trigger}"</span>
                        <ChevronRight className="w-3 h-3 text-[#948979]/50 shrink-0" />
                        <span className="text-[#948979] text-xs truncate flex-1">
                          {rule.useAI ? 'AI' : rule.response}
                        </span>
                        {rule.useAI && (
                          <Sparkles className="w-3 h-3 text-[#DFD0B8] shrink-0" />
                        )}
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </div>
            </div>
          </TabsContent>

          {/* API Setup */}
          <TabsContent value="api" className="space-y-4 sm:space-y-5 animate-fade-in mt-4">
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-6 gradient-border">
              <div className="flex items-center gap-3 mb-5">
                <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl sm:rounded-2xl bg-gradient-to-br from-[#DFD0B8]/20 to-[#DFD0B8]/5 flex items-center justify-center shrink-0">
                  <Key className="w-5 h-5 sm:w-6 sm:h-6 text-[#DFD0B8]" />
                </div>
                <div className="min-w-0">
                  <h2 className="text-base sm:text-lg font-semibold text-[#f5efe6]">API Configuration</h2>
                  <p className="text-[#948979] text-xs sm:text-sm truncate">Connect your AI provider</p>
                </div>
              </div>

              <div className="space-y-5">
                {/* API Key */}
                <div className="space-y-2">
                  <Label className="text-[#f5efe6] text-xs sm:text-sm font-medium flex items-center gap-2">
                    <Shield className="w-3.5 h-3.5 text-[#DFD0B8]" />
                    Gemini API Key
                  </Label>
                  <div className="relative">
                    <Input
                      type={showApiKey ? 'text' : 'password'}
                      placeholder="Enter API key..."
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      className="glass-input rounded-xl sm:rounded-2xl pr-20 sm:pr-24 h-11 sm:h-12 text-[#f5efe6] placeholder:text-[#948979]/50 text-sm"
                    />
                    <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1">
                      {apiKeyValid && (
                        <div className="reveal-animation flex items-center gap-1 bg-green-500/10 px-1.5 py-0.5 rounded">
                          <Check className="w-3 h-3 text-green-400" />
                          <span className="text-[10px] text-green-400">OK</span>
                        </div>
                      )}
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => setShowApiKey(!showApiKey)}
                        className="h-7 w-7 p-0 text-[#948979] hover:text-[#DFD0B8]"
                      >
                        {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </Button>
                    </div>
                  </div>
                  <p className="text-[10px] sm:text-xs text-[#948979]">
                    Get key from <a href="#" className="text-[#DFD0B8]">Google AI Studio</a>
                  </p>
                </div>

                {/* Models */}
                <div className="space-y-3">
                  <Label className="text-[#f5efe6] text-xs sm:text-sm font-medium flex items-center gap-2">
                    <Cpu className="w-3.5 h-3.5 text-[#DFD0B8]" />
                    AI Model
                  </Label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {[
                      { id: 'gemini-flash', name: 'Gemini 3 Flash', desc: 'Fast & Free', recommended: true },
                      { id: 'gemini-pro', name: 'Gemini 3 Pro', desc: 'More capable', recommended: false },
                    ].map((model) => (
                      <button
                        key={model.id}
                        className={cn(
                          "glass-card-light rounded-xl p-3 sm:p-4 text-left transition-all",
                          model.recommended && "border-[#DFD0B8]/20"
                        )}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-[#f5efe6] font-medium text-xs sm:text-sm">{model.name}</span>
                          {model.recommended && (
                            <Badge className="bg-[#DFD0B8]/20 text-[#DFD0B8] border-0 text-[10px] px-1.5 py-0">
                              Best
                            </Badge>
                          )}
                        </div>
                        <p className="text-[10px] sm:text-xs text-[#948979]">{model.desc}</p>
                      </button>
                    ))}
                  </div>
                </div>

                <Button 
                  className="glass-button-primary w-full rounded-xl sm:rounded-2xl h-11 sm:h-12 text-sm font-medium"
                  disabled={!apiKeyValid}
                >
                  <Check className="w-4 h-4 mr-2" />
                  Save Configuration
                </Button>
              </div>
            </div>

            {/* Offline Info */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 gradient-border border-amber-500/10">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-amber-500/10 flex items-center justify-center shrink-0">
                  <Smartphone className="w-4 h-4 sm:w-5 sm:h-5 text-amber-400" />
                </div>
                <div className="min-w-0">
                  <h3 className="font-semibold text-[#f5efe6] text-sm mb-1">Offline AI Option</h3>
                  <p className="text-[#948979] text-xs leading-relaxed">
                    Run Gemma 3 locally for complete privacy. Messages never leave your device.
                  </p>
                </div>
              </div>
            </div>
          </TabsContent>

          {/* Rules */}
          <TabsContent value="rules" className="space-y-4 sm:space-y-5 animate-fade-in mt-4">
            {/* Add Rule */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
              <div className="flex items-center gap-2 mb-4">
                <Plus className="w-4 h-4 text-[#DFD0B8]" />
                <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Create New Rule</h3>
              </div>
              
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label className="text-[#948979] text-xs sm:text-sm">Trigger Word</Label>
                  <Input
                    placeholder="e.g., meeting, price..."
                    value={newRule.trigger}
                    onChange={(e) => setNewRule({ ...newRule, trigger: e.target.value })}
                    className="glass-input rounded-xl h-10 sm:h-11 text-[#f5efe6] placeholder:text-[#948979]/50 text-sm"
                  />
                </div>
                
                <div className="space-y-2">
                  <Label className="text-[#948979] text-xs sm:text-sm">Response Type</Label>
                  <div className="flex gap-2">
                    <Button
                      variant={!newRule.useAI ? 'default' : 'outline'}
                      onClick={() => setNewRule({ ...newRule, useAI: false })}
                      className={cn(
                        "flex-1 rounded-xl h-10 sm:h-11 text-xs sm:text-sm",
                        !newRule.useAI ? "glass-button-primary" : "glass-button text-[#948979]"
                      )}
                    >
                      Custom
                    </Button>
                    <Button
                      variant={newRule.useAI ? 'default' : 'outline'}
                      onClick={() => setNewRule({ ...newRule, useAI: true })}
                      className={cn(
                        "flex-1 rounded-xl h-10 sm:h-11 text-xs sm:text-sm",
                        newRule.useAI ? "glass-button-primary" : "glass-button text-[#948979]"
                      )}
                    >
                      <Sparkles className="w-3.5 h-3.5 sm:mr-1" />
                      <span className="hidden sm:inline">AI</span>
                    </Button>
                  </div>
                </div>
                
                {!newRule.useAI && (
                  <div className="space-y-2 animate-fade-in">
                    <Label className="text-[#948979] text-xs sm:text-sm">Response Message</Label>
                    <Textarea
                      placeholder="Enter response..."
                      value={newRule.response}
                      onChange={(e) => setNewRule({ ...newRule, response: e.target.value })}
                      className="glass-input rounded-xl text-[#f5efe6] placeholder:text-[#948979]/50 min-h-[70px] text-sm resize-none"
                    />
                  </div>
                )}
                
                <Button 
                  onClick={handleAddRule}
                  disabled={!newRule.trigger.trim() || (!newRule.useAI && !newRule.response.trim())}
                  className="glass-button-primary w-full rounded-xl h-10 sm:h-11 text-sm font-medium"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Create Rule
                </Button>
              </div>
            </div>

            {/* Rules List */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
              <div className="flex items-center gap-2 mb-4">
                <ListChecks className="w-4 h-4 text-[#DFD0B8]" />
                <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Your Rules ({rules.length})</h3>
              </div>
              
              <ScrollArea className="h-[320px] sm:h-[400px]">
                <div className="space-y-2 pr-1">
                  {rules.map((rule) => (
                    <div 
                      key={rule.id} 
                      className={cn(
                        "glass-card-light rounded-xl p-3 sm:p-4 transition-all",
                        rule.enabled ? "" : "opacity-50"
                      )}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-1.5 flex-wrap mb-1">
                            <Badge 
                              variant="outline" 
                              className="border-[#DFD0B8]/20 text-[#f5efe6] bg-[#DFD0B8]/5 rounded-lg px-2 py-0.5 text-xs"
                            >
                              "{rule.trigger}"
                            </Badge>
                            {rule.useAI && (
                              <Badge className="bg-[#DFD0B8]/10 text-[#DFD0B8] border-0 rounded-lg px-1.5 py-0.5 text-[10px]">
                                AI
                              </Badge>
                            )}
                          </div>
                          <p className="text-[10px] sm:text-xs text-[#948979] truncate">
                            {rule.useAI ? 'AI generates response' : `→ ${rule.response}`}
                          </p>
                        </div>
                        
                        <div className="flex items-center gap-0.5 shrink-0">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleToggleRule(rule.id)}
                            className={cn(
                              "h-8 w-8 rounded-lg",
                              rule.enabled ? "text-green-400" : "text-[#948979]"
                            )}
                          >
                            {rule.enabled ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleDeleteRule(rule.id)}
                            className="h-8 w-8 rounded-lg text-[#948979] hover:text-red-400"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </div>
          </TabsContent>

          {/* Settings */}
          <TabsContent value="settings" className="space-y-4 sm:space-y-5 animate-fade-in mt-4">
            {/* Platforms */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
              <div className="flex items-center gap-2 mb-4">
                <Globe className="w-4 h-4 text-[#DFD0B8]" />
                <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Platforms</h3>
              </div>
              
              <div className="space-y-2">
                <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 bg-white/5">
                      <WhatsAppIcon className={cn("w-5 h-5", settings.replyWhatsApp ? "text-[#25D366]" : "text-[#948979]/50")} />
                    </div>
                    <span className={cn("text-sm truncate", settings.replyWhatsApp ? "text-[#f5efe6]" : "text-[#948979]")}>WhatsApp</span>
                  </div>
                  <Switch
                    checked={settings.replyWhatsApp}
                    onCheckedChange={(checked) => setSettings({ ...settings, replyWhatsApp: checked })}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-[#DFD0B8] data-[state=checked]:to-[#c4b49a] data-[state=unchecked]:bg-[#2f3744] shrink-0"
                  />
                </div>
                <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 bg-white/5">
                      <MessengerIcon className={cn("w-5 h-5", settings.replyMessenger ? "text-[#0099FF]" : "text-[#948979]/50")} />
                    </div>
                    <span className={cn("text-sm truncate", settings.replyMessenger ? "text-[#f5efe6]" : "text-[#948979]")}>Messenger</span>
                  </div>
                  <Switch
                    checked={settings.replyMessenger}
                    onCheckedChange={(checked) => setSettings({ ...settings, replyMessenger: checked })}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-[#DFD0B8] data-[state=checked]:to-[#c4b49a] data-[state=unchecked]:bg-[#2f3744] shrink-0"
                  />
                </div>
                <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 bg-white/5">
                      <TelegramIcon className={cn("w-5 h-5", settings.replyTelegram ? "text-[#26A5E4]" : "text-[#948979]/50")} />
                    </div>
                    <span className={cn("text-sm truncate", settings.replyTelegram ? "text-[#f5efe6]" : "text-[#948979]")}>Telegram</span>
                  </div>
                  <Switch
                    checked={settings.replyTelegram}
                    onCheckedChange={(checked) => setSettings({ ...settings, replyTelegram: checked })}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-[#DFD0B8] data-[state=checked]:to-[#c4b49a] data-[state=unchecked]:bg-[#2f3744] shrink-0"
                  />
                </div>
              </div>
            </div>

            {/* Persona */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
              <div className="flex items-center gap-2 mb-4">
                <Bot className="w-4 h-4 text-[#DFD0B8]" />
                <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">AI Persona</h3>
              </div>
              
              <div className="grid grid-cols-2 gap-2">
                {[
                  { id: 'professional', label: 'Pro', desc: 'Formal', icon: '💼' },
                  { id: 'friendly', label: 'Friendly', desc: 'Warm', icon: '😊' },
                  { id: 'witty', label: 'Witty', desc: 'Fun', icon: '✨' },
                  { id: 'minimal', label: 'Minimal', desc: 'Brief', icon: '⚡' },
                ].map((persona) => (
                  <button
                    key={persona.id}
                    onClick={() => setSettings({ ...settings, aiPersona: persona.id })}
                    className={cn(
                      "glass-card-light rounded-xl p-3 text-left transition-all",
                      settings.aiPersona === persona.id && "border-[#DFD0B8]/40 bg-[#DFD0B8]/5"
                    )}
                  >
                    <div className="text-lg sm:text-xl mb-1">{persona.icon}</div>
                    <span className="text-[#f5efe6] font-medium text-xs sm:text-sm">{persona.label}</span>
                    <p className="text-[10px] text-[#948979]">{persona.desc}</p>
                  </button>
                ))}
              </div>
            </div>

            {/* Timing */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 sm:p-5 gradient-border">
              <div className="flex items-center gap-2 mb-4">
                <Clock className="w-4 h-4 text-[#DFD0B8]" />
                <h3 className="font-semibold text-[#f5efe6] text-sm sm:text-base">Timing</h3>
              </div>
              
              <div className="space-y-4">
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label className="text-[#948979] text-xs sm:text-sm">Delay</Label>
                    <span className="text-[#DFD0B8] font-medium text-sm">{settings.autoDelay}s</span>
                  </div>
                  <Input
                    type="range"
                    min="0"
                    max="10"
                    value={settings.autoDelay}
                    onChange={(e) => setSettings({ ...settings, autoDelay: e.target.value })}
                    className="w-full"
                  />
                </div>
                
                <div className="flex items-center justify-between p-3 glass-card-light rounded-xl">
                  <div className="flex items-center gap-3 min-w-0">
                    {settings.quietHours ? <Moon className="w-4 h-4 text-[#DFD0B8] shrink-0" /> : <Sun className="w-4 h-4 text-[#948979] shrink-0" />}
                    <div>
                      <p className="text-[#f5efe6] text-sm">Quiet Hours</p>
                      <p className="text-[10px] text-[#948979]">No auto-replies</p>
                    </div>
                  </div>
                  <Switch
                    checked={settings.quietHours}
                    onCheckedChange={(checked) => setSettings({ ...settings, quietHours: checked })}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-[#DFD0B8] data-[state=checked]:to-[#c4b49a] data-[state=unchecked]:bg-[#2f3744] shrink-0"
                  />
                </div>
                
                {settings.quietHours && (
                  <div className="grid grid-cols-2 gap-3 animate-fade-in">
                    <div className="space-y-1">
                      <Label className="text-[#948979] text-xs">Start</Label>
                      <Input
                        type="time"
                        value={settings.quietStart}
                        onChange={(e) => setSettings({ ...settings, quietStart: e.target.value })}
                        className="glass-input rounded-xl h-10 text-[#f5efe6] text-sm"
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-[#948979] text-xs">End</Label>
                      <Input
                        type="time"
                        value={settings.quietEnd}
                        onChange={(e) => setSettings({ ...settings, quietEnd: e.target.value })}
                        className="glass-input rounded-xl h-10 text-[#f5efe6] text-sm"
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Permission */}
            <div className="glass-card rounded-2xl sm:rounded-3xl p-4 gradient-border border-amber-500/10">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-amber-500/10 flex items-center justify-center shrink-0">
                  <Shield className="w-4 h-4 sm:w-5 sm:h-5 text-amber-400" />
                </div>
                <div className="min-w-0 flex-1">
                  <h3 className="font-semibold text-[#f5efe6] text-sm mb-1">Permission Required</h3>
                  <p className="text-[#948979] text-xs leading-relaxed mb-3">
                    Enable notification access for auto-reply.
                  </p>
                  <Button className="glass-button rounded-xl h-9 text-xs px-4">
                    Open Settings
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </main>

      {/* Animated Footer - Fixed at bottom */}
      <footer className="sticky bottom-0 py-3 sm:py-4 text-center shrink-0 mt-auto safe-area-bottom">
        <div className="relative z-10">
          <button
            onClick={() => setShowAbout(true)}
            className="group glass-card hover:glass-card-light inline-flex items-center gap-2 px-4 py-2 sm:px-5 sm:py-2.5 rounded-full transition-all duration-300 hover:scale-105 active:scale-95"
          >
            <Bot className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-[#DFD0B8] group-hover:animate-pulse" />
            <span className="text-[#948979] group-hover:text-[#f5efe6] text-xs sm:text-sm font-medium transition-colors">AI Auto-Responder</span>
            <Info className="w-3 h-3 text-[#948979]/50 group-hover:text-[#DFD0B8] transition-colors" />
          </button>
        </div>
      </footer>

      {/* About Modal */}
      {showAbout && (
        <div
          className="fixed inset-0 z-[100] flex items-start justify-center p-3 sm:p-4 overflow-y-auto"
          onClick={() => setShowAbout(false)}
        >
          {/* Backdrop */}
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm animate-fade-in" />

          {/* Modal Content */}
          <div
            className="relative w-full max-w-sm glass-card rounded-3xl p-4 sm:p-5 my-4 sm:my-6 animate-scale-in safe-area-modal"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={() => setShowAbout(false)}
              className="absolute top-2.5 right-2.5 w-8 h-8 rounded-full glass-card-light flex items-center justify-center text-[#948979] hover:text-[#f5efe6] transition-colors z-10"
            >
              <X className="w-4 h-4" />
            </button>

            {/* App Info */}
            <div className="text-center mb-4 pt-1">
              <div className="w-12 h-12 sm:w-14 sm:h-14 mx-auto mb-2.5 glass-card rounded-2xl flex items-center justify-center float-animation">
                <Bot className="w-6 h-6 sm:w-7 sm:h-7 text-[#DFD0B8]" />
              </div>
              <h2 className="text-lg sm:text-xl font-bold text-[#f5efe6] mb-0.5">AI Auto-Responder</h2>
              <p className="text-[#948979] text-xs sm:text-sm">Intelligent Message Automation</p>
            </div>

            {/* About App */}
            <div className="glass-card-light rounded-2xl p-3 mb-3">
              <div className="flex items-center gap-2 mb-1.5">
                <Sparkles className="w-3.5 h-3.5 text-[#DFD0B8]" />
                <span className="text-[#f5efe6] font-medium text-xs sm:text-sm">About App</span>
              </div>
              <p className="text-[#948979] text-[11px] sm:text-xs leading-relaxed">
                AI Auto-Responder automatically replies to your messages across WhatsApp, Messenger, and Telegram using advanced AI. Customize responses, set rules, and let AI handle your conversations intelligently.
              </p>
            </div>

            {/* Creator Section */}
            <div className="glass-card-light rounded-2xl p-3 mb-3">
              <div className="flex items-center gap-2 mb-2.5">
                <User className="w-3.5 h-3.5 text-[#DFD0B8]" />
                <span className="text-[#f5efe6] font-medium text-xs sm:text-sm">Creator</span>
              </div>

              <div className="flex items-center gap-2.5 mb-3">
                <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-gradient-to-br from-[#DFD0B8] to-[#948979] flex items-center justify-center shrink-0">
                  <span className="text-[#1a1f26] font-bold text-xs sm:text-sm">RA</span>
                </div>
                <div>
                  <p className="text-[#f5efe6] font-semibold text-sm sm:text-base">RM ABIR</p>
                  <p className="text-[#948979] text-[11px]">Developer & Designer</p>
                </div>
              </div>

              {/* Contact Links */}
              <div className="space-y-1.5">
                <a
                  href="mailto:rahikulmakhtum147@gmail.com"
                  className="flex items-center gap-2.5 p-2 glass-card rounded-xl hover:bg-white/5 transition-all group"
                >
                  <div className="w-7 h-7 rounded-lg bg-amber-500/10 flex items-center justify-center shrink-0">
                    <Mail className="w-3.5 h-3.5 text-amber-400" />
                  </div>
                  <span className="text-[#f5efe6] text-[11px] sm:text-xs group-hover:text-[#DFD0B8] transition-colors truncate">rahikulmakhtum147@gmail.com</span>
                </a>

                <a
                  href="https://wa.me/8801919069898"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2.5 p-2 glass-card rounded-xl hover:bg-white/5 transition-all group"
                >
                  <div className="w-7 h-7 rounded-lg bg-[#25D366]/10 flex items-center justify-center shrink-0">
                    <WhatsAppIcon className="w-3.5 h-3.5 text-[#25D366]" />
                  </div>
                  <span className="text-[#f5efe6] text-[11px] sm:text-xs group-hover:text-[#DFD0B8] transition-colors">+8801919069898</span>
                  <ExternalLink className="w-3 h-3 text-[#948979] ml-auto shrink-0" />
                </a>

                <a
                  href="https://www.facebook.com/profile.php?id=61587401405859"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2.5 p-2 glass-card rounded-xl hover:bg-white/5 transition-all group"
                >
                  <div className="w-7 h-7 rounded-lg bg-[#1877F2]/10 flex items-center justify-center shrink-0">
                    <FacebookIcon className="w-3.5 h-3.5 text-[#1877F2]" />
                  </div>
                  <span className="text-[#f5efe6] text-[11px] sm:text-xs group-hover:text-[#DFD0B8] transition-colors">Facebook Profile</span>
                  <ExternalLink className="w-3 h-3 text-[#948979] ml-auto shrink-0" />
                </a>

                <a
                  href="https://www.instagram.com/rm_abir71"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2.5 p-2 glass-card rounded-xl hover:bg-white/5 transition-all group"
                >
                  <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-[#f09433]/20 via-[#dc2743]/20 to-[#bc1888]/20 flex items-center justify-center shrink-0">
                    <InstagramIcon className="w-3.5 h-3.5 text-[#E4405F]" />
                  </div>
                  <span className="text-[#f5efe6] text-[11px] sm:text-xs group-hover:text-[#DFD0B8] transition-colors">@rm_abir71</span>
                  <ExternalLink className="w-3 h-3 text-[#948979] ml-auto shrink-0" />
                </a>
              </div>
            </div>

            {/* Made with love */}
            <div className="flex items-center justify-center gap-1.5 text-[#948979] text-[11px] sm:text-xs pb-1">
              <span>Made with</span>
              <Heart className="w-3 h-3 text-red-400 animate-pulse" />
              <span>by RM ABIR</span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
