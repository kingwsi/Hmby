import { useAppStore } from '@/stores/app'
export const getItemImage = (item) => {
    const { config } = useAppStore();
    if (!config.emby_server) {
        console.warn('无法获取图片,emby_server未配置', item);
    }
    if (!item) {
        return null;
    }

    if (item.ImageTags && item.ImageTags.Thumb) {
        return `${config.emby_server}/emby/Items/${item.Id}/Images/Thumb?maxWidth=700&quality=100`;
    } else if (item.ImageTags && item.ImageTags.Primary) {
        return `${config.emby_server}/emby/Items/${item.Id}/Images/Primary?maxWidth=700&quality=100`;
    }
    console.warn('无法获取图片', item);
    return null;
}