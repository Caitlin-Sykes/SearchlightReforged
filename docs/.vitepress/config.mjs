import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "Searchlight Reforged",
  description: "Advanced lighting for Minecraft",
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Blocks', link: '/blocks/searchlight' }
    ],
    sidebar: [
      {
        text: 'Introduction',
        items: [
          { text: 'Home', link: '/' },
          { text: 'Integrations', link: '/integrations/' }
        ]
      },
      {
        text: 'Blocks',
        items: [
          { text: 'Searchlight', link: '/blocks/searchlight' },
          { text: 'Lighting Director', link: '/blocks/lighting-director' },
          {
            text: 'Light Blocks',
            link: '/blocks/',
            items: [
              { text: 'Wall Light', link: '/blocks/wall-light' },
              { text: 'Corner Light', link: '/blocks/corner-light' },
              { text: 'Centre Light', link: '/blocks/centre-light' },
              { text: 'Edge Light', link: '/blocks/edge-light' },
              { text: 'Colour Lamp', link: '/blocks/colour-lamp' }
              { text: 'Searchlight', link: '/blocks/searchlight' }
            ]
          }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/csykes/SearchlightReforged' }
    ]
  }
})
