import DefaultTheme from 'vitepress/theme'
import BlockIds from './components/BlockIds.vue'
import RecipeGrid from './components/RecipeGrid.vue'

export default {
    extends: DefaultTheme,

    enhanceApp({app}) {
        app.component('BlockIds', BlockIds)
        app.component('RecipeGrid', RecipeGrid)
    }
}