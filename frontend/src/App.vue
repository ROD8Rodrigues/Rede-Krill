<template>
  <Toast position="top-right" />
  <ConfirmDialog />
  <RouterView />
</template>

<script setup>
import { RouterView } from 'vue-router'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
</script>
