import DefaultTheme from 'vitepress/theme'
import BlockIds from './components/BlockIds.vue'

export default {
    extends: DefaultTheme,

    enhanceApp({ app }) {
        app.component('BlockIds', BlockIds)
    }
}