<!--
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
-->

<script setup lang="ts">
import { getTenantList, getTenantOverview } from '@/api'
import { usePagination } from '@/composables'
import { useHasLoading } from '@varlet/axle/use'
import { TenantTableService, TenantOverviewService, TenantFormService } from './components'

defineOptions({
  name: 'TenantView'
})

const { data: overview, loading: isOverviewLoading } = getTenantOverview().use()

const {
  data,
  getData: onLoadData,
  loading: isTableLoading
} = getTenantList().use({ immediate: false })

const { pagination, resetSearch, doSearch } = usePagination({
  onLoadData
})

const loading = useHasLoading(isOverviewLoading, isTableLoading)
</script>

<template>
  <n-spin :show="loading">
    <n-flex :style="{ gap: '24px' }" vertical>
      <TenantOverviewService :data="overview" />
      <TenantFormService @search="doSearch" @reset="resetSearch" />
      <TenantTableService :data="data?.tenantInfos" :pagination="pagination" />
    </n-flex>
  </n-spin>
</template>
