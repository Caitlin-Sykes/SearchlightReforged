import {defineConfig} from 'vitepress'

export default defineConfig({
    title: "Searchlight Reforged",
    description: "Lighting for Minecraft",
    lastUpdated: true,
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
                    {text: 'Home', link: '/'},
                    {text: 'Integrations', link: '/integrations/'}
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
