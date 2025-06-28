import { useAppStore } from '@/stores/app'
export const getThumb = (item) => {
    const { config } = useAppStore();
    if (!config.emby_server) {
        console.warn('无法获取图片,emby_server未配置', item);
    }
    if (!item) {
        return null;
    }
    return `${config.emby_server}/emby/Items/${item.Id}/Images/Thumb?maxWidth=700&quality=100`;
}

export const getPrimary = (item) => {
    const { config } = useAppStore();
    if (!config.emby_server) {
        console.warn('无法获取图片,emby_server未配置', item);
    }
    if (!item) {
        return null;
    }
    return `${config.emby_server}/emby/Items/${item.Id}/Images/Primary?maxWidth=700&quality=100`;
}