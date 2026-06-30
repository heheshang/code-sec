/** Repo list item — matches backend RepoListItem DTO. */
export interface RepoListItem {
  id: number
  name: string
  platform: string
  url: string
  businessLine: string
  status: string
  createdAt: string
}
