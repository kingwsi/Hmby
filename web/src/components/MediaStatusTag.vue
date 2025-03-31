<template>
  <div>
    <a-tag v-if="status === 'NONE'" color="#333">NONE</a-tag>
    <a-tag v-if="status === 'PENDING'" color="#827397">PENDING</a-tag>
    <a-tag v-if="status === 'PROCESSING'" color="#4B8673">PROCESSING</a-tag>
    <a-tag v-if="status === 'SUCCESS'" color="green">SUCCESS</a-tag>
    <a-tag v-if="status === 'FAIL'" color="red">FAIL</a-tag>
    <a-tag v-if="status === 'DELETED'" color="#666">DELETED</a-tag>
    <a-tag v-if="status === 'WAITING'">WAITING</a-tag>
    <a-tag v-if="status === 'DELETE_EMBY'" color="red">DELETE_EMBY</a-tag>
    <a-tag v-if="status === 'DONE'" color="#555">DONE</a-tag>
  </div>
</template>
<script>
export default {
  name: 'MediaStatusTag',
  props: {
    status: {
      type: String,
      default: '-999'
    }
  }
}
</script>
