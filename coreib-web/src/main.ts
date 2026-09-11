import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { registerPermissionDirectives } from './directives/permission'
import { registerElementPlus } from './plugins/element-plus'
import { useAppStore } from './stores/app-store'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
registerElementPlus(app)
registerPermissionDirectives(app)
useAppStore(pinia).initialize()
app.mount('#app')
