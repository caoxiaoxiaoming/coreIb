import type { App, Component } from 'vue'
import ElAlert from 'element-plus/es/components/alert/index'
import ElAvatar from 'element-plus/es/components/avatar/index'
import { ElBreadcrumb, ElBreadcrumbItem } from 'element-plus/es/components/breadcrumb/index'
import ElButton from 'element-plus/es/components/button/index'
import ElConfigProvider from 'element-plus/es/components/config-provider/index'
import ElDialog from 'element-plus/es/components/dialog/index'
import ElDrawer from 'element-plus/es/components/drawer/index'
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus/es/components/dropdown/index'
import ElEmpty from 'element-plus/es/components/empty/index'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index'
import ElIcon from 'element-plus/es/components/icon/index'
import ElInput from 'element-plus/es/components/input/index'
import ElLoading from 'element-plus/es/components/loading/index'
import { ElMenu, ElMenuItem, ElSubMenu } from 'element-plus/es/components/menu/index'
import ElScrollbar from 'element-plus/es/components/scrollbar/index'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index'
import ElSwitch from 'element-plus/es/components/switch/index'
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index'
import { ElTabPane, ElTabs } from 'element-plus/es/components/tabs/index'
import ElTag from 'element-plus/es/components/tag/index'
import ElTooltip from 'element-plus/es/components/tooltip/index'
import ElUpload from 'element-plus/es/components/upload/index'

import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/alert/style/css'
import 'element-plus/es/components/avatar/style/css'
import 'element-plus/es/components/breadcrumb/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/config-provider/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/drawer/style/css'
import 'element-plus/es/components/dropdown/style/css'
import 'element-plus/es/components/empty/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/menu/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/scrollbar/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/switch/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/tabs/style/css'
import 'element-plus/es/components/tag/style/css'
import 'element-plus/es/components/tooltip/style/css'
import 'element-plus/es/components/upload/style/css'

const components: Component[] = [
  ElAlert, ElAvatar, ElBreadcrumb, ElBreadcrumbItem, ElButton, ElConfigProvider,
  ElDialog, ElDrawer, ElDropdown, ElDropdownItem, ElDropdownMenu, ElEmpty,
  ElForm, ElFormItem, ElIcon, ElInput, ElMenu, ElMenuItem, ElOption, ElScrollbar,
  ElSelect, ElSubMenu, ElSwitch, ElTable, ElTableColumn, ElTabPane, ElTabs,
  ElTag, ElTooltip, ElUpload,
]

export function registerElementPlus(app: App) {
  components.forEach((component) => app.component(component.name!, component))
  app.use(ElLoading)
}
