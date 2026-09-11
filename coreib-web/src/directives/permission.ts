import type { App, DirectiveBinding, ObjectDirective } from 'vue'
import type { PermissionAction } from '../coreib-permissions'
import { usePermissionStore } from '../permission-store'

type PermissionBinding = [resource: string, action: PermissionAction] | {
  resource: string
  action: PermissionAction
}

function parse(binding: DirectiveBinding<PermissionBinding>): [string, PermissionAction] | null {
  if (Array.isArray(binding.value)) return binding.value
  if (binding.value?.resource && binding.value.action) return [binding.value.resource, binding.value.action]
  return null
}

const permissionDirective: ObjectDirective<HTMLElement, PermissionBinding> = {
  mounted(element, binding) {
    const requirement = parse(binding)
    const store = usePermissionStore()
    if (!requirement || !store.can(requirement[0], requirement[1])) element.remove()
  },
}

const roleDirective: ObjectDirective<HTMLElement, string[]> = {
  mounted(element, binding) {
    const store = usePermissionStore()
    if (!binding.value?.some((role) => store.roleCodes.includes(role))) element.remove()
  },
}

export function registerPermissionDirectives(app: App) {
  app.directive('permission', permissionDirective)
  app.directive('role', roleDirective)
}
