import fs from 'node:fs'
import path from 'node:path'
import {fileURLToPath} from 'node:url'
import {defineConfig} from 'vitepress'
import blockIds from './plugins/blockIds.mjs'
import {ViteImageOptimizer} from 'vite-plugin-image-optimizer';


const __dirname = path.dirname(fileURLToPath(import.meta.url))
const generatedDir = path.resolve(__dirname, '../generated')

function getCCTweakedGeneratedItems() {
    if (!fs.existsSync(generatedDir)) {
        return []
    }

    return fs
        .readdirSync(generatedDir)
        .filter(file => file.endsWith('.md'))
        .map(file => {
            const filePath = path.join(generatedDir, file)
            let title = file.replace(/\.java\.md$/, '').replace(/\.md$/, '')

            try {
                const content = fs.readFileSync(filePath, 'utf-8')
                const match = content.match(/^#\s+(.+)$/m)
                if (match) {
                    title = match[1].replace(/\s*-\s*CC:Tweaked API.*$/, '').trim()
                }
            } catch {
            }

            return {
                text: title,
                link: `/generated/${file}`
            }
        })
}

export default defineConfig({
    base: '/SearchlightReforged/',
    title: "Searchlight Reforged",
    description: "Lighting for Minecraft",
    lastUpdated: true,
    vite: {
        plugins: [
            ViteImageOptimizer({})
        ],
        resolve: {
            alias: {
                '@generated': generatedDir
            }
        }
    },
    markdown: {
        config(md) {
            md.use(blockIds)
        }
    },
    themeConfig: {
        search: {
            provider: 'local'
        },

        nav: [
            {text: 'Home', link: '/'},
            {text: 'Blocks', link: '/blocks/index.md'},
            {text: 'Items', link: '/items/index.md'}
        ],
        sidebar: [
            {
                items: [
                    {text: 'Home', link: '/'}
                ]
            },
            {
                text: 'Integrations',
                collapsed: false,
                items: [
                    {text: 'Overview', link: '/integrations/'},
                    {
                        text: 'CC: Tweaked',
                        items: getCCTweakedGeneratedItems()
                    }
                ]
            },
            {
                text: 'Blocks',
                items: [
                    {text: 'Lighting Director', link: '/blocks/lighting-director'},
                    {
                        text: 'Light Blocks',
                        link: '/blocks/',
                        items: [
                            {text: 'Wall Light', link: '/blocks/wall-light'},
                            {text: 'Corner Light', link: '/blocks/corner-light'},
                            {text: 'Centre Light', link: '/blocks/centre-light'},
                            {text: 'Edge Light', link: '/blocks/edge-light'},
                            {text: 'Colour Lamp', link: '/blocks/colour-lamp'},
                            {text: 'Colour Lamp Slab', link: '/blocks/colour-lamp-slab'},
                            {text: 'Searchlight', link: '/blocks/searchlight'},
                        ],
                    },
                ]
            },
            {
                text: 'Items',
                items: [
                    {text: 'Lighting Linker', link: '/items/lighting-linker'},

                ]
            }
        ],
        socialLinks: [
            {icon: 'github', link: 'https://github.com/csykes/SearchlightReforged'}
        ]
    }
})
